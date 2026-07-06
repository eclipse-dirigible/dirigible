/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.binding;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.eclipse.dirigible.parsers.document.Attributes;
import org.eclipse.dirigible.parsers.document.ForNode;
import org.eclipse.dirigible.parsers.document.IfNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.parser.NodeFactory;
import org.eclipse.dirigible.parsers.document.parser.TagRegistry;

/**
 * The data-binding layer that turns a parsed template into a data-shaped AST: Mustache placeholders
 * ({@code {{invoice.number}}}) in text and attribute values are substituted from a map-based
 * context, a {@code table}/{@code for} node's {@code source} list is expanded into one row per
 * element (each rendered in the row's scope), and an {@code if} node keeps or drops its children by
 * the truthiness of its {@code source}.
 *
 * <p>
 * The context is plain {@code Map<String, Object>} / {@code List<Object>} data (e.g. a JSON-decoded
 * entity). Paths walk nested maps ({@code customer.name}); inside a row scope a bare path
 * ({@code quantity}) resolves against the row first, then the enclosing document context. An
 * unresolved placeholder renders as an empty string — a printout must never show raw braces.
 */
public final class DataBinder {

    private final TagRegistry registry;

    /**
     * Creates a binder over the built-in tag registry.
     */
    public DataBinder() {
        this(TagRegistry.builtIn());
    }

    /**
     * Creates a binder that rebuilds nodes through a custom tag registry — required when the template
     * was parsed with registered extension tags.
     *
     * @param registry the registry the template was parsed with
     */
    public DataBinder(TagRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Binds the template to the data.
     *
     * @param root the parsed template root
     * @param data the document data context
     * @return a new AST with placeholders substituted and {@code table}/{@code for}/{@code if} expanded
     */
    public Node bind(Node root, Map<String, Object> data) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(data, "data");
        Scope scope = new Scope(data, null);
        return rebuild(root, scope, bindChildren(root, scope));
    }

    private List<Node> bindChildren(Node node, Scope scope) {
        List<Node> bound = new ArrayList<>();
        for (Node child : node.children()) {
            switch (child) {
                case IfNode ifNode -> {
                    if (isTruthy(scope.resolve(ifNode.attributes()
                                                     .get("source")))) {
                        bound.addAll(bindChildren(ifNode, scope));
                    }
                }
                case ForNode forNode -> expandRows(forNode, scope, bound);
                case TableNode table -> bound.add(expandTable(table, scope));
                default -> bound.add(rebuild(child, scope, bindChildren(child, scope)));
            }
        }
        return bound;
    }

    /**
     * A table keeps its column definitions (widths/labels for the renderer) and gains one {@code row}
     * of {@code column} cells per source element; each cell's content is the column template bound in
     * the row's scope.
     */
    private TableNode expandTable(TableNode table, Scope scope) {
        List<Node> children = new ArrayList<>();
        List<Node> columns = new ArrayList<>();
        for (Node child : table.children()) {
            if (child instanceof org.eclipse.dirigible.parsers.document.ColumnNode) {
                columns.add(child);
                children.add(rebuildWithoutText(child));
            }
        }
        for (Object element : asList(scope.resolve(table.attributes()
                                                        .get("source")))) {
            Scope rowScope = new Scope(asMap(element), scope);
            List<Node> cells = new ArrayList<>();
            for (Node column : columns) {
                cells.add(rebuild(column, rowScope, bindChildren(column, rowScope)));
            }
            children.add(new RowNode(table.position(), Attributes.EMPTY, cells, ""));
        }
        return new TableNode(table.position(), bindAttributes(table, scope), children, table.text());
    }

    private void expandRows(ForNode forNode, Scope scope, List<Node> target) {
        for (Object element : asList(scope.resolve(forNode.attributes()
                                                          .get("source")))) {
            Scope rowScope = new Scope(asMap(element), scope);
            target.addAll(bindChildren(forNode, rowScope));
        }
    }

    private Node rebuild(Node node, Scope scope, List<Node> children) {
        NodeFactory factory = registry.factory(node.tag());
        if (factory == null) {
            throw new IllegalStateException("Tag <" + node.tag() + "> is not registered in the binder's registry");
        }
        return factory.create(node.tag(), node.position(), bindAttributes(node, scope), children, substitute(node.text(), scope));
    }

    /** A column definition kept for the renderer: attributes stay, the cell template is dropped. */
    private Node rebuildWithoutText(Node column) {
        NodeFactory factory = registry.factory(column.tag());
        return factory.create(column.tag(), column.position(), column.attributes(), List.of(), "");
    }

    private Attributes bindAttributes(Node node, Scope scope) {
        if (node.attributes()
                .isEmpty()) {
            return Attributes.EMPTY;
        }
        List<Attributes.Attribute> bound = new ArrayList<>();
        for (Attributes.Attribute attribute : node.attributes()
                                                  .asList()) {
            bound.add(new Attributes.Attribute(attribute.name(), substitute(attribute.value(), scope), attribute.position()));
        }
        return Attributes.of(bound);
    }

    /** Replaces every {@code {{path}}} in the value; unresolved paths become empty strings. */
    private static String substitute(String value, Scope scope) {
        if (value == null || !value.contains("{{")) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        int cursor = 0;
        while (true) {
            int open = value.indexOf("{{", cursor);
            if (open < 0) {
                result.append(value, cursor, value.length());
                return result.toString();
            }
            int close = value.indexOf("}}", open + 2);
            if (close < 0) {
                result.append(value, cursor, value.length());
                return result.toString();
            }
            result.append(value, cursor, open);
            Object resolved = scope.resolve(value.substring(open + 2, close)
                                                 .trim());
            if (resolved != null) {
                result.append(stringify(resolved));
            }
            cursor = close + 2;
        }
    }

    /**
     * Floating-point values print in the generated forms' money pattern ({@code ### ### ### ##0.00} —
     * thousands grouped by a space, two decimals), locale-independent for deterministic output; a
     * {@code Map} (a relation/object node) prints its {@code __label} value so a bare
     * {@code {{document.Customer}}} still renders the display label while
     * {@code {{document.Customer.Address}}} descends into the same node; every other value prints via
     * {@code toString}.
     */
    private static String stringify(Object resolved) {
        if (resolved instanceof Double || resolved instanceof Float || resolved instanceof BigDecimal) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
            symbols.setGroupingSeparator(' ');
            return new DecimalFormat("###,###,###,##0.00", symbols).format(resolved);
        }
        if (resolved instanceof Map<?, ?> map) {
            Object label = map.get("__label");
            return label == null ? "" : stringify(label);
        }
        return String.valueOf(resolved);
    }

    private static boolean isTruthy(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean bool -> bool;
            case String string -> !string.isBlank() && !string.equalsIgnoreCase("false");
            case Number number -> number.doubleValue() != 0;
            case List<?> list -> !list.isEmpty();
            case Map<?, ?> map -> !map.isEmpty();
            default -> true;
        };
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    /** A row scope resolving against its own data first, then the enclosing scope. */
    private record Scope(Map<String, Object> data, Scope parent) {

        Object resolve(String path) {
            if (path == null || path.isBlank()) {
                return null;
            }
            Object resolved = resolveLocal(path.trim());
            return resolved == null && parent != null ? parent.resolve(path) : resolved;
        }

        private Object resolveLocal(String path) {
            Object current = data;
            for (String segment : path.split("\\.")) {
                if (!(current instanceof Map<?, ?> map)) {
                    return null;
                }
                current = map.get(segment);
            }
            return current;
        }
    }
}
