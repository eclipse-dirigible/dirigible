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

/**
 * A 1-based line and column location in the template source, attached to every AST node and
 * attribute so that later validation and rendering errors can point at the exact spot.
 *
 * @param line the 1-based line number
 * @param column the 1-based column number
 */
public record SourcePosition(int line, int column) {

    /**
     * Creates a position.
     *
     * @param line the 1-based line number
     * @param column the 1-based column number
     * @return the position
     */
    public static SourcePosition of(int line, int column) {
        return new SourcePosition(line, column);
    }

    /**
     * The human-readable form used in error messages.
     *
     * @return {@code "line L, column C"}
     */
    @Override
    public String toString() {
        return "line " + line + ", column " + column;
    }
}
