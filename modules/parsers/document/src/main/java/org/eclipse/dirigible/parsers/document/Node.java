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
 * A node of the document-template AST — the declarative widget tree of a printable business
 * document. Every node carries its tag name, its attributes (raw strings), its child nodes in
 * document order, its normalized text content, and its source position.
 *
 * <p>
 * The hierarchy is sealed over one record per built-in tag so that consumers (e.g. the layout
 * engine) can pattern-match exhaustively; {@link CustomNode} is the non-sealed extension point for
 * tags registered at runtime.
 */
public sealed interface Node permits DocumentNode, PageNode, HeaderNode, FooterNode, SectionNode, RowNode, ColumnNode, StackNode, TextNode,
        FieldNode, TableNode, ImageNode, LineNode, SpaceNode, TotalNode, IfNode, ForNode, CustomNode {

    /**
     * The tag name, e.g. {@code "table"}.
     *
     * @return the tag name
     */
    String tag();

    /**
     * The attributes declared on the tag.
     *
     * @return the attributes, never {@code null}
     */
    Attributes attributes();

    /**
     * The child nodes in document order.
     *
     * @return an unmodifiable list, never {@code null}
     */
    List<Node> children();

    /**
     * The normalized text content: all text segments concatenated, whitespace runs collapsed to single
     * spaces and trimmed. Mustache placeholders are preserved verbatim.
     *
     * @return the text, never {@code null} (empty when the node has no text)
     */
    String text();

    /**
     * The position of the node's opening tag in the template source.
     *
     * @return the position
     */
    SourcePosition position();
}
