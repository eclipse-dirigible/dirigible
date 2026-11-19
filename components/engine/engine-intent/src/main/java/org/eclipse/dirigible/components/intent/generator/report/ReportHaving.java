/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The grammar of a report's {@code having:} (dirigible #7914), shared by the parser that validates
 * it and the generator that rewrites it: comparisons over aggregate calls - {@code count(*)},
 * {@code sum(total)}, {@code max(Customer.limit)} - joined by {@code AND} / {@code OR}, with
 * numbers and quoted literals. A HAVING clause filters GROUPS, so a bare field outside an aggregate
 * call has no single value there; it is refused at parse rather than left for the database to
 * reject.
 */
public final class ReportHaving {

    /**
     * One aggregate call - the function in group 1, its argument ({@code *}, empty or a field path) in
     * group 2. The argument cannot nest parentheses, which keeps the scan linear.
     */
    public static final Pattern AGGREGATE_CALL = Pattern.compile("\\b(count|sum|avg|min|max)\\s*\\(([^()]*)\\)", Pattern.CASE_INSENSITIVE);

    /** The words a HAVING clause may carry besides its aggregate calls. */
    private static final Set<String> KEYWORDS = Set.of("AND", "OR", "NOT", "IS", "NULL", "BETWEEN", "IN", "TRUE", "FALSE");

    private ReportHaving() {}

    /**
     * The field paths the clause aggregates over, in order - never {@code *} or an empty argument.
     *
     * @param having the authored clause
     * @return the aggregated paths
     */
    public static List<String> aggregatedPaths(String having) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = AGGREGATE_CALL.matcher(having);
        while (matcher.find()) {
            String argument = matcher.group(2)
                                     .trim();
            if (!argument.isEmpty() && !"*".equals(argument)) {
                paths.add(argument);
            }
        }
        return paths;
    }

    /**
     * The identifiers the clause names OUTSIDE an aggregate call and a quoted literal that are not one
     * of its keywords - each one a column a group has no single value of.
     *
     * @param having the authored clause
     * @return the stray identifiers, empty when the clause is well-formed
     */
    public static List<String> strayIdentifiers(String having) {
        String rest = AGGREGATE_CALL.matcher(having)
                                    .replaceAll(" 0 ");
        List<String> stray = new ArrayList<>();
        int i = 0;
        while (i < rest.length()) {
            char character = rest.charAt(i);
            if (character == '\'') {
                int end = rest.indexOf('\'', i + 1);
                i = end < 0 ? rest.length() : end + 1;
            } else if (Character.isLetter(character) || character == '_') {
                int end = i + 1;
                while (end < rest.length()
                        && (Character.isLetterOrDigit(rest.charAt(end)) || rest.charAt(end) == '_' || rest.charAt(end) == '.')) {
                    end++;
                }
                String word = rest.substring(i, end);
                if (!KEYWORDS.contains(word.toUpperCase(Locale.ROOT))) {
                    stray.add(word);
                }
                i = end;
            } else if (Character.isDigit(character)) {
                // A number - its digits, a decimal point or an exponent are no identifier.
                int end = i + 1;
                while (end < rest.length() && (Character.isLetterOrDigit(rest.charAt(end)) || rest.charAt(end) == '.')) {
                    end++;
                }
                i = end;
            } else {
                i++;
            }
        }
        return stray;
    }
}
