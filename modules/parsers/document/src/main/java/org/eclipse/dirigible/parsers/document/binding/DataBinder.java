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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
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
 * Rows can be filtered declaratively — no expressions, staying a value match: a {@code table} or
 * {@code for} node with {@code filter="Kind"} keeps only the elements whose {@code Kind} resolves
 * truthy, and adding {@code match="CONTRIBUTION | TAX"} narrows that to the listed literal values
 * ({@code |}-separated, trimmed). The same {@code match} attribute on an {@code if} node compares
 * its resolved {@code source} against the listed values instead of testing truthiness. One fed
 * collection can this way render into several purpose-grouped tables (a payslip's earnings vs
 * deductions, a journal entry's debit vs credit side) without pre-splitting the data.
 *
 * <p>
 * The context is plain {@code Map<String, Object>} / {@code List<Object>} data (e.g. a JSON-decoded
 * entity). Paths walk nested maps ({@code customer.name}); inside a row scope a bare path
 * ({@code quantity}) resolves against the row first, then the enclosing document context. An
 * unresolved placeholder renders as an empty string — a printout must never show raw braces.
 *
 * <p>
 * A placeholder may list <b>alternative</b> paths separated by {@code |} — the first one resolving
 * to a non-blank value wins, left to right, and the last one is rendered whatever it holds. That is
 * the only fallback syntax (no literals, no expressions), and it exists because an optional twin
 * field is the normal shape of business data: a locally registered
 * {@code &#123;&#123;document.Customer.NameLocal|document.Customer.Name&#125;&#125;} prints the
 * local name when the record has one and the canonical name when it does not, instead of leaving a
 * hole in a legal document.
 *
 * <p>
 * An operand may also carry an explicit <b>format</b> after its first {@code :} — a
 * {@code DecimalFormat} pattern for numbers ({@code &#123;&#123;Price:#,##0.00&#125;&#125;}), a
 * {@code DateTimeFormatter} pattern for temporals and their ISO string form
 * ({@code &#123;&#123;document.Date:dd.MM.yyyy&#125;&#125;}). It exists because the default number
 * rendering cannot know a bare integral JSON value ({@code 5390}) is money that lost its scale on
 * the way through the browser — only the template author knows, and says so per placeholder.
 */
public final class DataBinder {

    /**
     * The generated forms' money symbols: ROOT locale (deterministic output), thousands grouped by a
     * space. {@code DecimalFormat} clones the symbols it is constructed with, so sharing is safe.
     */
    private static final DecimalFormatSymbols MONEY_SYMBOLS = moneySymbols();

    private static DecimalFormatSymbols moneySymbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        return symbols;
    }

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
                    Object condition = scope.resolve(ifNode.attributes()
                                                           .get("source"));
                    if (matches(condition, ifNode.attributes()
                                                 .get("match"))) {
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
            if (!keepRow(table, rowScope)) {
                continue;
            }
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
            if (!keepRow(forNode, rowScope)) {
                continue;
            }
            target.addAll(bindChildren(forNode, rowScope));
        }
    }

    /**
     * The row filter of a {@code table}/{@code for} node: no {@code filter} attribute keeps every row;
     * {@code filter="<path>"} alone keeps the rows where the path resolves truthy (in the row's scope);
     * {@code filter} plus {@code match="A | B"} keeps the rows whose resolved value equals one of the
     * listed literals.
     */
    private static boolean keepRow(Node node, Scope rowScope) {
        String filter = node.attributes()
                            .get("filter");
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return matches(rowScope.resolve(filter.trim()), node.attributes()
                                                            .get("match"));
    }

    /**
     * No {@code match} list means plain truthiness; otherwise the value's string form must equal one of
     * the {@code |}-separated, trimmed literals — a null never matches.
     */
    private static boolean matches(Object value, String match) {
        if (match == null || match.isBlank()) {
            return isTruthy(value);
        }
        if (value == null) {
            return false;
        }
        String candidate = String.valueOf(value);
        for (String literal : match.split("\\|")) {
            if (literal.trim()
                       .equals(candidate)) {
                return true;
            }
        }
        return false;
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
            result.append(resolvePlaceholder(value.substring(open + 2, close), scope));
            cursor = close + 2;
        }
    }

    /**
     * One placeholder body: a single path, or {@code |}-separated <b>alternative</b> paths of which the
     * first one resolving to a non-blank value wins, left to right ({@code
     * &#123;&#123;document.Customer.NameLocal|document.Customer.Name&#125;&#125;} - print the locally
     * registered name when there is one, else the canonical name). "Blank" is null, missing, or
     * whitespace-only. The LAST operand is always rendered, whatever it resolves to, so a lone path
     * behaves exactly as it always has (all operands blank renders empty) and only the alternatives
     * before it can be skipped.
     *
     * <p>
     * An operand may carry an explicit <b>format</b> after its first {@code :} - the pattern applied to
     * that operand's resolved value: a {@link DecimalFormat} pattern for a number
     * ({@code &#123;&#123;Price:#,##0.00&#125;&#125;} prints a whole-figure {@code 5390} as
     * {@code 5 390.00}, which the default rendering cannot - it never sees the lost scale of an
     * integral JSON number), or a {@link DateTimeFormatter} pattern for a temporal or its ISO string
     * form ({@code &#123;&#123;document.Date:dd.MM.yyyy&#125;&#125;}). The first colon splits, so a
     * time pattern keeps its own colons ({@code &#123;&#123;At:HH:mm&#125;&#125;}). A pattern the value
     * cannot satisfy - or a value of any other type - falls back to the default rendering: a printout
     * never shows an exception, matching the parser's leniency contract.
     */
    private static String resolvePlaceholder(String body, Scope scope) {
        String[] operands = body.split("\\|");
        for (int i = 0; i < operands.length; i++) {
            String operand = operands[i];
            String pattern = null;
            int colon = operand.indexOf(':');
            if (colon >= 0) {
                pattern = operand.substring(colon + 1)
                                 .trim();
                operand = operand.substring(0, colon);
            }
            Object resolved = scope.resolve(operand.trim());
            String rendered = resolved == null ? "" : stringify(resolved, pattern);
            if (i == operands.length - 1 || !rendered.isBlank()) {
                return rendered;
            }
        }
        return "";
    }

    /**
     * Floating-point values print in the generated forms' money pattern ({@code ### ### ### ##0.00} —
     * thousands grouped by a space, two decimals), locale-independent for deterministic output; a
     * {@code Map} (a relation/object node) prints its {@code __label} value so a bare
     * {@code {{document.Customer}}} still renders the display label while
     * {@code {{document.Customer.Address}}} descends into the same node; every other value prints via
     * {@code toString}. An explicit placeholder format takes precedence when the value fits it (see
     * {@link #resolvePlaceholder(String, Scope)}).
     */
    private static String stringify(Object resolved, String pattern) {
        if (resolved instanceof Map<?, ?> map) {
            Object label = map.get("__label");
            return label == null ? "" : stringify(label, pattern);
        }
        if (pattern != null && !pattern.isBlank()) {
            String formatted = tryFormat(resolved, pattern);
            if (formatted != null) {
                return formatted;
            }
        }
        if (resolved instanceof Double || resolved instanceof Float || resolved instanceof BigDecimal) {
            return new DecimalFormat("###,###,###,##0.00", MONEY_SYMBOLS).format(resolved);
        }
        return String.valueOf(resolved);
    }

    /**
     * The explicit format of one operand: a {@link DecimalFormat} pattern over any {@link Number}
     * (integral ones included - the reason the specifier exists), a {@link DateTimeFormatter} pattern
     * over a temporal or the ISO string a feeder emits for one. {@code null} when the value is of any
     * other shape or cannot satisfy the pattern - the caller then renders the default way.
     */
    private static String tryFormat(Object resolved, String pattern) {
        try {
            if (resolved instanceof Number number) {
                return new DecimalFormat(pattern, MONEY_SYMBOLS).format(number);
            }
            TemporalAccessor temporal = asTemporal(resolved);
            if (temporal != null) {
                return DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
                                        .format(temporal);
            }
        } catch (RuntimeException ex) {
            // an invalid pattern, or one asking for fields the value does not carry
        }
        return null;
    }

    /** A temporal as-is, or an ISO date / date-time string parsed back into one; else {@code null}. */
    private static TemporalAccessor asTemporal(Object resolved) {
        if (resolved instanceof TemporalAccessor temporal) {
            return temporal;
        }
        if (resolved instanceof String string) {
            String candidate = string.trim();
            try {
                return LocalDate.parse(candidate);
            } catch (DateTimeParseException notADate) {
                // fall through to the date-time shapes
            }
            try {
                return LocalDateTime.parse(candidate);
            } catch (DateTimeParseException notALocalDateTime) {
                // fall through
            }
            try {
                return OffsetDateTime.parse(candidate);
            } catch (DateTimeParseException notATemporal) {
                // not a temporal string
            }
        }
        return null;
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
