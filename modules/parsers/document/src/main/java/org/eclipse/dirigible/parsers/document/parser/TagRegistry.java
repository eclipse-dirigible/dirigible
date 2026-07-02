/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.parser;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.parsers.document.ColumnNode;
import org.eclipse.dirigible.parsers.document.CustomNode;
import org.eclipse.dirigible.parsers.document.DocumentNode;
import org.eclipse.dirigible.parsers.document.FieldNode;
import org.eclipse.dirigible.parsers.document.FooterNode;
import org.eclipse.dirigible.parsers.document.ForNode;
import org.eclipse.dirigible.parsers.document.HeaderNode;
import org.eclipse.dirigible.parsers.document.IfNode;
import org.eclipse.dirigible.parsers.document.ImageNode;
import org.eclipse.dirigible.parsers.document.LineNode;
import org.eclipse.dirigible.parsers.document.PageNode;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.SectionNode;
import org.eclipse.dirigible.parsers.document.SpaceNode;
import org.eclipse.dirigible.parsers.document.StackNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.TextNode;
import org.eclipse.dirigible.parsers.document.TotalNode;

/**
 * The tag-name-to-factory registry the parser resolves elements against. Tag names are
 * case-sensitive lowercase. Unknown tags are a parse error — a typo like {@code <colum>} in a
 * printable document must fail fast rather than silently drop content — so extending the DSL is an
 * explicit, one-line registration:
 *
 * <pre>
 * TagRegistry registry = TagRegistry.builtIn();
 * registry.register("qrcode"); // parses to CustomNode
 * </pre>
 */
public final class TagRegistry {

    private final Map<String, NodeFactory> factories = new LinkedHashMap<>();

    private TagRegistry() {}

    /**
     * Creates a registry pre-populated with the built-in tags: document, page, header, footer, section,
     * row, column, stack, text, field, table, image, line, space, total, if, for.
     *
     * @return the registry
     */
    public static TagRegistry builtIn() {
        TagRegistry registry = new TagRegistry();
        registry.register("document",
                (tag, position, attributes, children, text) -> new DocumentNode(position, attributes, children, text));
        registry.register("page", (tag, position, attributes, children, text) -> new PageNode(position, attributes, children, text));
        registry.register("header", (tag, position, attributes, children, text) -> new HeaderNode(position, attributes, children, text));
        registry.register("footer", (tag, position, attributes, children, text) -> new FooterNode(position, attributes, children, text));
        registry.register("section", (tag, position, attributes, children, text) -> new SectionNode(position, attributes, children, text));
        registry.register("row", (tag, position, attributes, children, text) -> new RowNode(position, attributes, children, text));
        registry.register("column", (tag, position, attributes, children, text) -> new ColumnNode(position, attributes, children, text));
        registry.register("stack", (tag, position, attributes, children, text) -> new StackNode(position, attributes, children, text));
        registry.register("text", (tag, position, attributes, children, text) -> new TextNode(position, attributes, children, text));
        registry.register("field", (tag, position, attributes, children, text) -> new FieldNode(position, attributes, children, text));
        registry.register("table", (tag, position, attributes, children, text) -> new TableNode(position, attributes, children, text));
        registry.register("image", (tag, position, attributes, children, text) -> new ImageNode(position, attributes, children, text));
        registry.register("line", (tag, position, attributes, children, text) -> new LineNode(position, attributes, children, text));
        registry.register("space", (tag, position, attributes, children, text) -> new SpaceNode(position, attributes, children, text));
        registry.register("total", (tag, position, attributes, children, text) -> new TotalNode(position, attributes, children, text));
        registry.register("if", (tag, position, attributes, children, text) -> new IfNode(position, attributes, children, text));
        registry.register("for", (tag, position, attributes, children, text) -> new ForNode(position, attributes, children, text));
        return registry;
    }

    /**
     * Registers an extension tag that parses to {@link CustomNode}.
     *
     * @param tag the tag name
     */
    public void register(String tag) {
        register(tag, CustomNode::new);
    }

    /**
     * Registers an extension tag with a custom node factory, replacing any previous registration.
     *
     * @param tag the tag name
     * @param factory the factory producing the node
     */
    public void register(String tag, NodeFactory factory) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory must not be null");
        }
        factories.put(tag, factory);
    }

    /**
     * Whether the tag is registered.
     *
     * @param tag the tag name
     * @return {@code true} when registered
     */
    public boolean isRegistered(String tag) {
        return factories.containsKey(tag);
    }

    /**
     * The factory for a registered tag.
     *
     * @param tag the tag name
     * @return the factory, or {@code null} when the tag is not registered
     */
    public NodeFactory factory(String tag) {
        return factories.get(tag);
    }
}
