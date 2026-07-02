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

import org.eclipse.dirigible.parsers.document.SourcePosition;

/**
 * A character-level cursor over the template source with 1-based line/column tracking. All position
 * arithmetic lives here so every {@link ParseException} carries a correct location.
 */
final class Scanner {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    Scanner(String source) {
        this.source = source;
    }

    boolean eof() {
        return pos >= source.length();
    }

    char peek() {
        return source.charAt(pos);
    }

    boolean lookingAt(String prefix) {
        return source.startsWith(prefix, pos);
    }

    char next() {
        char c = source.charAt(pos++);
        // \n and a lone \r advance the line; the \n of a \r\n pair does the advancing for both
        if (c == '\n' || (c == '\r' && (pos >= source.length() || source.charAt(pos) != '\n'))) {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    void skip(int count) {
        for (int i = 0; i < count; i++) {
            next();
        }
    }

    int line() {
        return line;
    }

    int column() {
        return column;
    }

    SourcePosition position() {
        return SourcePosition.of(line, column);
    }

    ParseException error(String message) {
        return new ParseException(message, line, column);
    }

    ParseException errorAt(String message, SourcePosition position) {
        return new ParseException(message, position.line(), position.column());
    }
}
