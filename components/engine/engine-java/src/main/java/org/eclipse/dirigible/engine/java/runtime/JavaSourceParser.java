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
 * Lightweight, regex-based extractor for the {@code package} declaration and primary top-level type
 * name from a Java source string.
 *
 * <p>
 * We deliberately avoid pulling in a full Java parser; the synchronizer only needs the binary class
 * name to key the artefact and request a compilation unit by name. Comments and literals are
 * stripped first so {@code //}- and {@code /* *}/-embedded {@code package} tokens don't trip up the
 * matcher.
 *
 * <p>
 * The stripping is a single linear character scan, not a set of regex passes, for two reasons. A
 * regex alternation under a quantifier ({@code "(?:\\.|[^"\\])*"}) costs Java's engine one stack
 * frame per iteration, so a long enough string literal - a generated report's multi-kilobyte SQL
 * constant full of escaped identifier quotes - overflows the thread stack; and running the comment
 * passes before the literal pass gets the nesting backwards, so a {@code /*} inside a string
 * literal opened a comment that swallowed real code up to the next {@code *}{@code /} in the file.
 */
public final class JavaSourceParser {

    private static final Pattern PACKAGE_DECL = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;");

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

    /**
     * Replace every comment and literal with an inert placeholder, in one left-to-right pass. Each
     * construct is recognised where it starts, so a comment opener inside a literal is literal text and
     * a quote inside a comment is comment text - the ordering a set of independent regex passes cannot
     * express. Line structure outside block comments is preserved, which is what the {@code (?m)^\s*}
     * anchor of {@link #PACKAGE_DECL} reads.
     *
     * @param source raw Java source
     * @return the source with comments and literals neutralised
     */
    private static String stripCommentsAndLiterals(String source) {
        int length = source.length();
        StringBuilder stripped = new StringBuilder(length);
        int index = 0;
        while (index < length) {
            char current = source.charAt(index);
            char following = index + 1 < length ? source.charAt(index + 1) : '\0';
            if (current == '/' && following == '/') {
                index = skipLineComment(source, index);
                stripped.append(' ');
            } else if (current == '/' && following == '*') {
                index = skipBlockComment(source, index);
                stripped.append(' ');
            } else if (current == '"' && following == '"' && index + 2 < length && source.charAt(index + 2) == '"') {
                index = skipTextBlock(source, index);
                stripped.append("\"\"");
            } else if (current == '"') {
                index = skipQuoted(source, index, '"');
                stripped.append("\"\"");
            } else if (current == '\'') {
                index = skipQuoted(source, index, '\'');
                stripped.append("' '");
            } else {
                stripped.append(current);
                index++;
            }
        }
        return stripped.toString();
    }

    /** Index of the terminating newline, which the caller keeps, or the end of the source. */
    private static int skipLineComment(String source, int start) {
        int index = start + 2;
        while (index < source.length() && source.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    /** Index just past the closing delimiter, or the end of an unterminated comment. */
    private static int skipBlockComment(String source, int start) {
        int end = source.indexOf("*/", start + 2);
        return end < 0 ? source.length() : end + 2;
    }

    /** Index just past the closing triple quote, or the end of an unterminated text block. */
    private static int skipTextBlock(String source, int start) {
        int length = source.length();
        int index = start + 3;
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
            } else if (current == '"' && index + 2 < length && source.charAt(index + 1) == '"' && source.charAt(index + 2) == '"') {
                return index + 3;
            } else {
                index++;
            }
        }
        return length;
    }

    /**
     * Index just past the closing quote of a string or character literal. Neither may span a line, so
     * an unterminated one stops at the newline rather than consuming the rest of the source.
     */
    private static int skipQuoted(String source, int start, char quote) {
        int length = source.length();
        int index = start + 1;
        while (index < length) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
            } else if (current == quote) {
                return index + 1;
            } else if (current == '\n') {
                return index;
            } else {
                index++;
            }
        }
        return length;
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
