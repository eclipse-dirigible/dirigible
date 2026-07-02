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

/**
 * The horizontal alignment values of the document DSL's {@code align} attribute.
 */
public enum Alignment {

    /** Align to the left edge — the default. */
    LEFT,
    /** Center horizontally. */
    CENTER,
    /** Align to the right edge. */
    RIGHT,
    /** Stretch text to both edges. */
    JUSTIFY;

    /**
     * Parses a raw attribute value, case-insensitively.
     *
     * @param raw the value as authored; {@code null} and blank parse to {@link #LEFT}
     * @return the alignment
     * @throws IllegalArgumentException when the value is not a valid alignment
     */
    public static Alignment parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LEFT;
        }
        return switch (raw.trim()
                          .toLowerCase()) {
            case "left" -> LEFT;
            case "center" -> CENTER;
            case "right" -> RIGHT;
            case "justify" -> JUSTIFY;
            default -> throw new IllegalArgumentException("Invalid alignment: '" + raw + "'");
        };
    }
}
