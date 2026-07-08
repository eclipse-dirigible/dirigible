/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Evaluator for calculated-field formulas in generated entity repositories. A single neutral
 * arithmetic expression - authored once on a model property - is evaluated here on the server and
 * by the identical {@code harmoniaCalcEval} mirror in the generated UI, so a document's line totals
 * preview on the client exactly as the server will persist them.
 * <p>
 * Grammar (a closed arithmetic language - no member access, method calls, statements or
 * assignment):
 *
 * <pre>
 *   expr   := term (('+' | '-') term)*
 *   term   := factor (('*' | '/') factor)*
 *   factor := NUMBER | IDENT | '(' expr ')' | ('-' | '+') factor | func
 *   func   := IDENT '(' expr (',' expr)* ')'
 * </pre>
 *
 * An {@code IDENT} names a property of the {@code entity} and is read from its public field;
 * functions are limited to {@code round(x, n)}, {@code abs(x)}, {@code min(a, b)},
 * {@code max(a, b)}, {@code ceil(x)}, {@code floor(x)} and the date functions
 * {@code daysBetween(a, b)} (calendar days, {@code b - a}), {@code businessDaysBetween(a, b)}
 * (Mon-Fri dates in the closed interval {@code [a, b]}, {@code 0} when {@code b < a}) and
 * {@code monthsBetween(a, b)} (whole calendar months).
 * <p>
 * A <b>date-typed</b> identifier ({@code java.time} date/datetime, {@code java.util.Date},
 * {@code java.sql} date types, or an ISO {@code yyyy-MM-dd[...]} string) reads as its <b>epoch
 * day</b>, which is what the date functions consume - so
 * {@code businessDaysBetween(FromDate, ToDate)} computes working days between two date fields, and
 * {@code daysBetween(FromDate, ToDate) + 1} is the inclusive span.
 * <p>
 * Semantics contract (kept identical to the JS mirror): a {@code null}, missing or non-numeric
 * identifier reads as {@code 0}; division by zero yields {@code 0}; rounding is half-up with ties
 * away from zero ({@link RoundingMode#HALF_UP}). Arithmetic is performed in {@code double} -
 * matching the client's JavaScript number semantics so both sides agree to the last digit - and
 * only the final result is rounded to the property's scale and returned as a {@link BigDecimal}.
 */
public final class Calc {

    private Calc() {}

    /**
     * Evaluate a calculated-field expression against an entity and round to the given scale.
     *
     * @param expression the neutral arithmetic formula (e.g. {@code "Quantity * Price"})
     * @param entity the entity whose public fields supply the identifier values
     * @param scale the number of decimal places of the target property
     * @return the computed value rounded to {@code scale}, or {@link BigDecimal#ZERO} (at scale) for a
     *         blank expression
     */
    public static BigDecimal eval(String expression, Object entity, int scale) {
        double result = new Parser(expression, entity).evaluate();
        return BigDecimal.valueOf(roundHalfUp(result, scale))
                         .setScale(scale, RoundingMode.HALF_UP);
    }

    /** Half-up rounding with ties away from zero - the exact rule the JS mirror applies. */
    private static double roundHalfUp(double value, int decimals) {
        if (!Double.isFinite(value)) {
            return 0d;
        }
        double factor = Math.pow(10, decimals);
        double sign = value < 0 ? -1d : 1d;
        return sign * Math.floor(Math.abs(value) * factor + 0.5d) / factor;
    }

    /**
     * A single-pass recursive-descent evaluator. Not thread-safe and intentionally short-lived - one
     * instance per {@link #eval} call.
     */
    private static final class Parser {

        private final String source;
        private final Object entity;
        private int pos;

        private Parser(String expression, Object entity) {
            this.source = expression == null ? "" : expression;
            this.entity = entity;
        }

        private double evaluate() {
            return parseExpr();
        }

        private void skipWhitespace() {
            while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            return pos < source.length() ? source.charAt(pos) : '\0';
        }

        private double parseExpr() {
            double value = parseTerm();
            for (;;) {
                char c = peek();
                if (c == '+') {
                    pos++;
                    value += parseTerm();
                } else if (c == '-') {
                    pos++;
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            for (;;) {
                char c = peek();
                if (c == '*') {
                    pos++;
                    value *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    double divisor = parseFactor();
                    value = divisor == 0d ? 0d : value / divisor;
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            char c = peek();
            if (c == '-') {
                pos++;
                return -parseFactor();
            }
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            if (c == '(') {
                pos++;
                double value = parseExpr();
                skipWhitespace();
                if (pos < source.length() && source.charAt(pos) == ')') {
                    pos++;
                }
                return value;
            }
            if ((c >= '0' && c <= '9') || c == '.') {
                return parseNumber();
            }
            return parseIdentifierOrFunction();
        }

        private double parseNumber() {
            skipWhitespace();
            int start = pos;
            while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                pos++;
            }
            try {
                return Double.parseDouble(source.substring(start, pos));
            } catch (NumberFormatException e) {
                return 0d;
            }
        }

        private double parseIdentifierOrFunction() {
            skipWhitespace();
            int start = pos;
            while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                pos++;
            }
            String name = source.substring(start, pos);
            if (name.isEmpty()) {
                // Unrecognized character - skip it so a malformed expression yields 0 rather than looping.
                pos++;
                return 0d;
            }
            if (peek() == '(') {
                pos++;
                double first = peek() == ')' ? 0d : parseExpr();
                double second = 0d;
                boolean hasSecond = false;
                while (peek() == ',') {
                    pos++;
                    second = parseExpr();
                    hasSecond = true;
                }
                skipWhitespace();
                if (pos < source.length() && source.charAt(pos) == ')') {
                    pos++;
                }
                return applyFunction(name, first, second, hasSecond);
            }
            return readField(name);
        }

        private double applyFunction(String name, double a, double b, boolean hasSecond) {
            switch (name) {
                case "round":
                    return roundHalfUp(a, hasSecond ? (int) b : 0);
                case "abs":
                    return Math.abs(a);
                case "min":
                    return Math.min(a, b);
                case "max":
                    return Math.max(a, b);
                case "ceil":
                    return Math.ceil(a);
                case "floor":
                    return Math.floor(a);
                case "daysBetween":
                    return Math.floor(b) - Math.floor(a);
                case "businessDaysBetween":
                    return businessDaysBetween((long) Math.floor(a), (long) Math.floor(b));
                case "monthsBetween":
                    return monthsBetween((long) Math.floor(a), (long) Math.floor(b));
                default:
                    return 0d;
            }
        }

        /**
         * Mon-Fri dates in the closed interval of the two epoch days; {@code 0} when the interval is empty.
         * Counted arithmetically (no per-day loop) so a years-long span stays O(1): epoch day 0
         * (1970-01-01) was a Thursday, so {@code floorMod(epochDay + 3, 7)} is the weekday with Monday = 0
         * - the JS mirror derives the same index from {@code getUTCDay}.
         */
        private static double businessDaysBetween(long from, long to) {
            if (to < from) {
                return 0d;
            }
            long days = to - from + 1;
            long fullWeeks = days / 7;
            long count = fullWeeks * 5;
            long remainder = days % 7;
            long startWeekday = Math.floorMod(from + 3, 7); // Monday = 0 ... Sunday = 6
            for (long i = 0; i < remainder; i++) {
                if ((startWeekday + i) % 7 < 5) {
                    count++;
                }
            }
            return count;
        }

        /** Whole calendar months between the two epoch days ({@code b - a} in year*12+month terms). */
        private static double monthsBetween(long from, long to) {
            java.time.LocalDate a = java.time.LocalDate.ofEpochDay(from);
            java.time.LocalDate b = java.time.LocalDate.ofEpochDay(to);
            return (b.getYear() - a.getYear()) * 12 + (b.getMonthValue() - a.getMonthValue());
        }

        /**
         * Read an identifier from the entity's public field; null / missing / non-numeric reads as 0. A
         * date-typed value reads as its epoch day so the date functions can consume it.
         */
        private double readField(String name) {
            if (entity == null) {
                return 0d;
            }
            try {
                Field field = entity.getClass()
                                    .getField(name);
                Object value = field.get(entity);
                if (value == null) {
                    return 0d;
                }
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                Double epochDay = toEpochDay(value);
                if (epochDay != null) {
                    return epochDay;
                }
                String text = value.toString()
                                   .trim();
                if (text.isEmpty()) {
                    return 0d;
                }
                return Double.parseDouble(text);
            } catch (NoSuchFieldException | IllegalAccessException | NumberFormatException e) {
                return 0d;
            }
        }

        /**
         * The epoch day of a date-shaped value, or {@code null} when the value is not date-shaped. An ISO
         * {@code yyyy-MM-dd} prefix covers the string forms the HTML date/datetime inputs and the JSON
         * serializations produce - matching the JS mirror's coercion.
         */
        private static Double toEpochDay(Object value) {
            if (value instanceof java.time.LocalDate localDate) {
                return (double) localDate.toEpochDay();
            }
            if (value instanceof java.time.LocalDateTime localDateTime) {
                return (double) localDateTime.toLocalDate()
                                             .toEpochDay();
            }
            if (value instanceof java.time.Instant instant) {
                return (double) instant.atZone(java.time.ZoneOffset.UTC)
                                       .toLocalDate()
                                       .toEpochDay();
            }
            if (value instanceof java.util.Date date) {
                // covers java.sql.Date / java.sql.Timestamp too
                return (double) java.time.Instant.ofEpochMilli(date.getTime())
                                                 .atZone(java.time.ZoneOffset.UTC)
                                                 .toLocalDate()
                                                 .toEpochDay();
            }
            if (value instanceof String text) {
                java.util.regex.Matcher m = ISO_DATE_PREFIX.matcher(text.trim());
                if (m.find()) {
                    try {
                        return (double) java.time.LocalDate.parse(m.group())
                                                           .toEpochDay();
                    } catch (java.time.format.DateTimeParseException e) {
                        return null;
                    }
                }
            }
            return null;
        }

        private static final java.util.regex.Pattern ISO_DATE_PREFIX = java.util.regex.Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");
    }
}
