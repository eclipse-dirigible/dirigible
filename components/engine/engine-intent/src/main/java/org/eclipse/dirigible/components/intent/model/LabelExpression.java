/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The parsed form of an entity {@code label:} expression - literals interleaved with
 * {@code {field}} / {@code {Relation.field}} tokens, each token optionally carrying a
 * {@code |format} (a {@code DateTimeFormatter} pattern for temporal values). Shared by the parser
 * (validation) and the EDM generator (template-ready parts).
 */
public final class LabelExpression {

    /** One segment of the expression: a literal, an own-field token, or a one-hop relation token. */
    public record Part(String literal, String relation, String property, String format) {

        /** Whether this part is a literal text segment. */
        public boolean isLiteral() {
            return literal != null;
        }
    }

    private LabelExpression() {}

    /**
     * Parse a label expression into its parts.
     *
     * @param expression the authored expression, e.g. {@code "{number} - {Customer.name}"}
     * @return the parts, in order
     * @throws IllegalArgumentException on unbalanced braces, an empty token, or a path deeper than one
     *         relation hop
     */
    public static List<Part> parse(String expression) {
        List<Part> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '}') {
                throw new IllegalArgumentException("unbalanced '}' at position " + i);
            }
            if (c != '{') {
                literal.append(c);
                continue;
            }
            int end = expression.indexOf('}', i);
            if (end < 0) {
                throw new IllegalArgumentException("unbalanced '{' at position " + i);
            }
            if (literal.length() > 0) {
                parts.add(new Part(literal.toString(), null, null, null));
                literal.setLength(0);
            }
            String token = expression.substring(i + 1, end)
                                     .trim();
            i = end;
            String format = null;
            int pipe = token.indexOf('|');
            if (pipe >= 0) {
                format = token.substring(pipe + 1)
                              .trim();
                token = token.substring(0, pipe)
                             .trim();
            }
            if (token.isEmpty()) {
                throw new IllegalArgumentException("empty token in label expression");
            }
            String[] path = token.split("\\.");
            if (path.length == 1) {
                parts.add(new Part(null, null, path[0], format));
            } else if (path.length == 2) {
                parts.add(new Part(null, path[0], path[1], format));
            } else {
                throw new IllegalArgumentException(
                        "label token [" + token + "] is deeper than one relation hop - compose via the related entity's own label instead");
            }
        }
        if (literal.length() > 0) {
            parts.add(new Part(literal.toString(), null, null, null));
        }
        return parts;
    }
}
