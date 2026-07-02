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

/**
 * Signals a syntax error in a document template. The message always names the exact 1-based line
 * and column, e.g. {@code "Mismatched closing tag: expected </row>, found </column> at line 12,
 * column 5"}. Unchecked because template errors are end-user data errors surfaced at a single parse
 * call site.
 */
public class ParseException extends RuntimeException {

    /** The serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The 1-based line of the error. */
    private final int line;

    /** The 1-based column of the error. */
    private final int column;

    /**
     * Creates the exception.
     *
     * @param message the problem description, without position suffix
     * @param line the 1-based line of the error
     * @param column the 1-based column of the error
     */
    public ParseException(String message, int line, int column) {
        super(message + " at line " + line + ", column " + column);
        this.line = line;
        this.column = column;
    }

    /**
     * The 1-based line of the error.
     *
     * @return the line
     */
    public int getLine() {
        return line;
    }

    /**
     * The 1-based column of the error.
     *
     * @return the column
     */
    public int getColumn() {
        return column;
    }
}
