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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The relative moments a report {@code filter:} may compare a date with (dirigible #8435), so a
 * report can cover "this year" or "the last 30 days" without a parameter:
 * <ul>
 * <li>{@code CURRENT_DATE} - today, as in plain SQL;</li>
 * <li>{@code START_OF_YEAR}, {@code START_OF_QUARTER}, {@code START_OF_MONTH} - the first day of
 * the current period;</li>
 * <li>any of the above with one signed ISO-8601 date period - {@code CURRENT_DATE-P30D},
 * {@code START_OF_YEAR-P1Y} (the start of last year), {@code CURRENT_DATE+P1M2W} - the notation a
 * {@code schedules[].where} moment already uses (#6764). Years, months, weeks and days only: a
 * report filter compares dates, so a time part ({@code PT1H}) is refused rather than rounded.</li>
 * </ul>
 * The {@code .report} query is a static string with no dialect to switch on, so each moment is
 * emitted as standard SQL verified on both CI databases (H2, PostgreSQL): the period start as
 * {@code DATE_TRUNC('YEAR', CURRENT_DATE)}, the offset as {@code INTERVAL 'n' DAY} arithmetic (the
 * form the ageing buckets already use), the whole cast back to a {@code DATE} - {@code DATE_TRUNC}
 * answers a timestamp on PostgreSQL. A bare {@code CURRENT_DATE} is left exactly as written.
 * Moments inside a quoted string literal are text, not moments, and are never touched.
 */
public final class ReportMoments {

    /** The period-start tokens and the {@code DATE_TRUNC} unit each truncates today to. */
    private static final Map<String, String> PERIOD_STARTS =
            Map.of("START_OF_YEAR", "YEAR", "START_OF_QUARTER", "QUARTER", "START_OF_MONTH", "MONTH");

    /**
     * A moment: the token in group 1, then optionally the sign in group 2 and the period text in group
     * 3. A {@code START_OF_<word>} of any spelling is matched so a misspelt unit is reported rather
     * than passed through to the database as an unknown column. The word is matched as
     * underscore-separated alphanumeric segments, possessively: one way to match any input, so the scan
     * stays linear however the filter is spelt (the CodeQL polynomial-regex alert on {@code \w+} after
     * the {@code START_OF_} prefix).
     */
    private static final Pattern MOMENT = Pattern.compile(
            "(?<![\\w.\"])(CURRENT_DATE|START_OF_[A-Za-z0-9]++(?:_[A-Za-z0-9]++)*+)(?:\\s*+([+-])\\s*+(P[A-Za-z0-9]*+))?(?![\\w.\"])",
            Pattern.CASE_INSENSITIVE);

    /**
     * A date period: years, months, weeks, days - at least one, each a non-negative integer of at most
     * five digits. The bound keeps {@link Long#parseLong} from overflowing on an absurd period, which
     * would otherwise escape the parser as a raw {@link NumberFormatException} instead of an authoring
     * issue - and no report window is 100,000 days long.
     */
    private static final Pattern PERIOD =
            Pattern.compile("P(?:(\\d{1,5})Y)?(?:(\\d{1,5})M)?(?:(\\d{1,5})W)?(?:(\\d{1,5})D)?", Pattern.CASE_INSENSITIVE);

    private ReportMoments() {}

    /**
     * The authoring problems of the moments in a filter: an unknown period start, or an offset that is
     * not a date period.
     *
     * @param filter the report filter, may be null
     * @return one message per problem, empty when every moment is well-formed
     */
    public static List<String> problems(String filter) {
        List<String> problems = new ArrayList<>();
        if (filter == null || filter.isBlank()) {
            return problems;
        }
        Matcher matcher = MOMENT.matcher(blankLiterals(filter));
        while (matcher.find()) {
            String token = matcher.group(1)
                                  .toUpperCase(Locale.ROOT);
            if (!"CURRENT_DATE".equals(token) && !PERIOD_STARTS.containsKey(token)) {
                problems.add("references unknown moment [" + matcher.group(1) + "] - expected CURRENT_DATE or one of "
                        + List.of("START_OF_YEAR", "START_OF_QUARTER", "START_OF_MONTH"));
                continue;
            }
            if (matcher.group(3) != null && interval(matcher.group(2), matcher.group(3)) == null) {
                problems.add("moment [" + matcher.group()
                                                 .trim()
                        + "] has an offset that is not a date period - expected an ISO-8601 period of years, months, weeks"
                        + " and days such as P30D, P1M or P1Y");
            }
        }
        return problems;
    }

    /**
     * Rewrite every relative moment of a filter to its SQL. A well-formed filter is assumed (see
     * {@link #problems}); a malformed moment is left as written.
     *
     * @param filter the report filter, may be null
     * @return the filter with its moments as SQL
     */
    public static String rewrite(String filter) {
        if (filter == null || filter.isBlank()) {
            return filter;
        }
        Matcher matcher = MOMENT.matcher(blankLiterals(filter));
        StringBuilder result = new StringBuilder(filter.length());
        int last = 0;
        while (matcher.find()) {
            String sql = sql(matcher.group(1), matcher.group(2), matcher.group(3));
            if (sql == null) {
                continue;
            }
            result.append(filter, last, matcher.start())
                  .append(sql);
            last = matcher.end();
        }
        return result.append(filter, last, filter.length())
                     .toString();
    }

    /** The SQL of one moment, or null when it is left as written. */
    private static String sql(String token, String sign, String period) {
        String upper = token.toUpperCase(Locale.ROOT);
        String base;
        if ("CURRENT_DATE".equals(upper)) {
            if (period == null) {
                return null;
            }
            base = "CURRENT_DATE";
        } else if (PERIOD_STARTS.containsKey(upper)) {
            base = "DATE_TRUNC('" + PERIOD_STARTS.get(upper) + "', CURRENT_DATE)";
        } else {
            return null;
        }
        if (period == null) {
            return "CAST(" + base + " AS DATE)";
        }
        String interval = interval(sign, period);
        return interval == null ? null : "CAST(" + base + interval + " AS DATE)";
    }

    /** The {@code  - INTERVAL 'n' UNIT} chain of a signed period, or null when it is not one. */
    private static String interval(String sign, String period) {
        Matcher matcher = PERIOD.matcher(period);
        if (!matcher.matches()) {
            return null;
        }
        long years = number(matcher.group(1));
        long months = number(matcher.group(2));
        long days = number(matcher.group(3)) * 7 + number(matcher.group(4));
        if (matcher.group(1) == null && matcher.group(2) == null && matcher.group(3) == null && matcher.group(4) == null) {
            return null;
        }
        StringBuilder sql = new StringBuilder();
        append(sql, sign, years, "YEAR");
        append(sql, sign, months, "MONTH");
        append(sql, sign, days, "DAY");
        return sql.toString();
    }

    private static void append(StringBuilder sql, String sign, long amount, String unit) {
        if (amount > 0) {
            sql.append(' ')
               .append(sign)
               .append(" INTERVAL '")
               .append(amount)
               .append("' ")
               .append(unit);
        }
    }

    private static long number(String digits) {
        return digits == null ? 0 : Long.parseLong(digits);
    }

    /**
     * The filter with the inside of every single-quoted literal blanked to spaces - same length, so a
     * match position maps back onto the original text - so a moment spelt inside a string value is
     * never taken for one. A doubled quote inside a literal is SQL's escaped quote.
     */
    private static String blankLiterals(String filter) {
        char[] chars = filter.toCharArray();
        boolean inLiteral = false;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\'') {
                if (inLiteral && i + 1 < chars.length && chars[i + 1] == '\'') {
                    chars[i] = ' ';
                    chars[++i] = ' ';
                    continue;
                }
                inLiteral = !inLiteral;
            } else if (inLiteral) {
                chars[i] = ' ';
            }
        }
        return new String(chars);
    }
}
