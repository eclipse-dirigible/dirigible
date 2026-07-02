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
 * AST node for the {@code <image>} tag.
 *
 * @param position the position of the opening tag in the source
 * @param attributes the attributes declared on the tag
 * @param children the child nodes in document order
 * @param text the normalized text content
 */
public record ImageNode(SourcePosition position, Attributes attributes, List<Node> children, String text) implements Node {

    /**
     * Normalizes null components and defensively copies the children.
     *
     * @param position the position of the opening tag in the source
     * @param attributes the attributes declared on the tag
     * @param children the child nodes in document order
     * @param text the normalized text content
     */
    public ImageNode {
        attributes = attributes == null ? Attributes.EMPTY : attributes;
        children = children == null ? List.of() : List.copyOf(children);
        text = text == null ? "" : text;
    }

    @Override
    public String tag() {
        return "image";
    }
}
