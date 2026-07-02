/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document;

import java.util.List;

/**
 * The extension node for tags registered at runtime (e.g. {@code <qrcode>}). It is the only
 * non-sealed member of the {@link Node} hierarchy: a class rather than a record so that custom node
 * factories may subclass it with richer, tag-specific types.
 */
public non-sealed class CustomNode implements Node {

    private final String tag;
    private final SourcePosition position;
    private final Attributes attributes;
    private final List<Node> children;
    private final String text;

    /**
     * Creates a custom node.
     *
     * @param tag the tag name, must not be blank
     * @param position the position of the opening tag in the source
     * @param attributes the attributes declared on the tag
     * @param children the child nodes in document order
     * @param text the normalized text content
     */
    public CustomNode(String tag, SourcePosition position, Attributes attributes, List<Node> children, String text) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        this.tag = tag;
        this.position = position;
        this.attributes = attributes == null ? Attributes.EMPTY : attributes;
        this.children = children == null ? List.of() : List.copyOf(children);
        this.text = text == null ? "" : text;
    }

    @Override
    public String tag() {
        return tag;
    }

    @Override
    public SourcePosition position() {
        return position;
    }

    @Override
    public Attributes attributes() {
        return attributes;
    }

    @Override
    public List<Node> children() {
        return children;
    }

    @Override
    public String text() {
        return text;
    }
}
