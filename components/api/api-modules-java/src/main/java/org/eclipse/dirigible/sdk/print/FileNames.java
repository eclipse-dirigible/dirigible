/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.print;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Builds the file name of a server-side render - the snapshot copy a document mints on issue, the
 * PDF a notify block attaches. The pieces a generated delegate interpolates go through here so that
 * a value coming out of business data can never produce a name the file system, the CMS or a mail
 * client would reject.
 *
 * <p>
 * The three operations are exactly the three the declarative {@code fileName:} pattern needs: a
 * sanitized value ({@link #part(Object)}), a sanitized date rendered in an authored pattern
 * ({@link #part(Object, String)}), and the first non-blank of several alternatives
 * ({@link #first(String...)}).
 */
public final class FileNames {

    /**
     * The characters a file name may not carry, across the file system, the CMS path syntax and a mail
     * client's attachment handling: the two path separators, the drive/stream colon, the glob
     * wildcards, the quote, the redirection brackets and the alternation bar. Removed, not replaced - a
     * substitute character would only look like part of the value.
     */
    private static final String FORBIDDEN = "/\\:*?\"<>|";

    private FileNames() {}

    /**
     * Render one interpolated value as a file-name part: trimmed, stripped of the characters a name may
     * not carry, with internal whitespace turned into a single underscore.
     *
     * <p>
     * Non-ASCII characters are deliberately KEPT. A document in a local language legitimately carries a
     * non-Latin name, and keeping names Latin is an application's data convention - not something the
     * platform may guess on its behalf by mangling the value.
     *
     * @param value the resolved value, may be {@code null}
     * @return the sanitized part, empty when the value is {@code null} or blank
     */
    public static String part(Object value) {
        if (value == null) {
            return "";
        }
        StringBuilder stripped = new StringBuilder();
        for (char character : String.valueOf(value)
                                    .trim()
                                    .toCharArray()) {
            if (Character.isWhitespace(character)) {
                stripped.append(' ');
            } else if (character >= ' ' && FORBIDDEN.indexOf(character) < 0) {
                stripped.append(character);
            }
        }
        // Whitespace (including the runs a stripped character left behind) collapses to one separator,
        // and a separator at either end is dropped - the author owns the literal separators, so a value
        // must never contribute one of its own.
        return stripped.toString()
                       .trim()
                       .replaceAll("\\s+", "_");
    }

    /**
     * Render a date/time value in an authored pattern, then sanitize it as {@link #part(Object)} does.
     * A value that is not a date/time falls back to its plain sanitized form, so a pattern authored
     * against a field whose type later changes degrades to the value instead of failing a mint.
     *
     * @param value the resolved value, may be {@code null}
     * @param pattern a {@link DateTimeFormatter} pattern, e.g. {@code yyyyMMdd}
     * @return the sanitized part, empty when the value is {@code null} or blank
     */
    public static String part(Object value, String pattern) {
        if (value == null || pattern == null || pattern.isBlank()) {
            return part(value);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (value instanceof TemporalAccessor temporal) {
            return part(formatter.format(temporal));
        }
        if (value instanceof Date date) {
            return part(formatter.format(date.toInstant()
                                             .atZone(java.time.ZoneId.systemDefault())));
        }
        return part(value);
    }

    /**
     * The first non-blank of several already-sanitized alternatives - the {@code A|B} operand list of a
     * {@code fileName:} pattern, where an optional twin field (a short name beside the legal name) is
     * filled for some records and not for others.
     *
     * @param candidates the alternatives, in authored order
     * @return the first non-blank one, or empty when they all are
     */
    public static String first(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}
