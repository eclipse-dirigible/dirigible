/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, regex-based extractor for the {@code package} declaration and primary top-level
 * type name from a Java source string.
 *
 * <p>
 * We deliberately avoid pulling in a full Java parser; the synchronizer only needs the binary
 * class name to key the artefact and request a compilation unit by name. Comments and string
 * literals are stripped first so {@code //}- and {@code /* *}/-embedded {@code package} tokens
 * don't trip up the matcher.
 */
public final class JavaSourceParser {

    // Strip /* ... */ block comments (non-greedy, multi-line) and // line comments.
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern CHAR_LITERAL = Pattern.compile("'(?:\\\\.|[^'\\\\])'");

    private static final Pattern PACKAGE_DECL =
            Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;");

    // First class/interface/record/enum declaration after optional modifiers.
    // Sub-expression captures the simple name.
    private static final Pattern TYPE_DECL =
            Pattern.compile("\\b(?:public\\s+|final\\s+|abstract\\s+|static\\s+|sealed\\s+|non-sealed\\s+)*"
                    + "(?:class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");

    private JavaSourceParser() {}

    /**
     * Parse the package and primary type name. The returned FQN is suitable for passing to
     * {@link javax.tools.JavaCompiler}.
     *
     * @param source raw Java source
     * @return parsed coordinates
     * @throws JavaSourceParseException if no top-level type declaration can be found
     */
    public static ParsedSource parse(String source) {
        String stripped = stripCommentsAndLiterals(source);

        String packageName = "";
        Matcher pkgMatcher = PACKAGE_DECL.matcher(stripped);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1);
        }

        Matcher typeMatcher = TYPE_DECL.matcher(stripped);
        if (!typeMatcher.find()) {
            throw new JavaSourceParseException("No top-level class/interface/record/enum declaration found");
        }
        String simpleName = typeMatcher.group(1);
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        return new ParsedSource(packageName, simpleName, fqn);
    }

    private static String stripCommentsAndLiterals(String source) {
        String s = BLOCK_COMMENT.matcher(source)
                                .replaceAll(" ");
        s = LINE_COMMENT.matcher(s)
                        .replaceAll(" ");
        s = STRING_LITERAL.matcher(s)
                          .replaceAll("\"\"");
        s = CHAR_LITERAL.matcher(s)
                        .replaceAll("' '");
        return s;
    }

    /** Parsed coordinates of a Java source. */
    public record ParsedSource(String packageName, String simpleName, String fqn) {
    }

    /** Thrown when a source cannot be parsed for its primary type name. */
    public static final class JavaSourceParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JavaSourceParseException(String message) {
            super(message);
        }
    }

}
