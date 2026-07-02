/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.layout;

import java.util.List;

import org.eclipse.dirigible.parsers.document.Node;

/**
 * One node of the normalized layout tree: the source AST node plus its resolved measurements and
 * alignment. Carries no geometry — coordinates, pagination and text measurement belong to a
 * concrete renderer.
 *
 * @param source the AST node this layout node was derived from
 * @param width the resolved width
 * @param height the resolved height
 * @param alignment the resolved horizontal alignment
 * @param children the child layout nodes in document order
 */
public record LayoutNode(Node source, Measurement width, Measurement height, Alignment alignment, List<LayoutNode> children) {

    /**
     * Defensively copies the children.
     *
     * @param source the AST node this layout node was derived from
     * @param width the resolved width
     * @param height the resolved height
     * @param alignment the resolved horizontal alignment
     * @param children the child layout nodes in document order
     */
    public LayoutNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
