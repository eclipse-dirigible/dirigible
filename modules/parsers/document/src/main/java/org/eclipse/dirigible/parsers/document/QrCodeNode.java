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
 * AST node for the {@code <qrcode>} tag - a QR code printed from a text payload.
 *
 * <p>
 * The payload is the element's text, which - unlike every other tag's - keeps its <b>line
 * breaks</b>: the payloads worth printing (the EPC069-12 "SEPA credit transfer" payment code of an
 * invoice) are line-oriented and their empty lines are significant. The parser normalizes that text
 * only as far as the template's own formatting goes (see
 * {@code TagRegistry.registerLinePreserving}). A single-line payload may be written as the
 * {@code value} attribute instead, which wins when it is not blank.
 *
 * @param position the position of the opening tag in the source
 * @param attributes the attributes declared on the tag
 * @param children the child nodes in document order
 * @param text the line-preserving text content
 */
public record QrCodeNode(SourcePosition position, Attributes attributes, List<Node> children, String text) implements Node {

    /** The tag name. */
    public static final String TAG = "qrcode";

    /**
     * Normalizes null components and defensively copies the children.
     *
     * @param position the position of the opening tag in the source
     * @param attributes the attributes declared on the tag
     * @param children the child nodes in document order
     * @param text the line-preserving text content
     */
    public QrCodeNode {
        attributes = attributes == null ? Attributes.EMPTY : attributes;
        children = children == null ? List.of() : List.copyOf(children);
        text = text == null ? "" : text;
    }

    @Override
    public String tag() {
        return TAG;
    }

    /**
     * The text the QR code carries: the {@code value} attribute when it is not blank, else the
     * element's text with its line breaks.
     *
     * @return the payload, never {@code null}; blank when there is nothing to encode
     */
    public String payload() {
        String value = attributes.getOrDefault("value", "");
        return value.isBlank() ? text : value;
    }
}
