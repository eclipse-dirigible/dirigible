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
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Evaluator for calculated-field formulas in generated entity repositories. A single neutral
 * arithmetic expression - authored once on a model property - is evaluated here on the server and
 * by the {@code harmoniaCalcEval} mirror in the generated UI, which previews a document's line
 * totals on the client as the server will persist them.
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
 * An {@code IDENT} names a property of the {@code entity} and is read from its public field - or,
 * when the entity is a {@link Map}, from the entry under that key, so a formula can also be
 * evaluated over a record that has no compiled type (the generated mapping code does this);
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
 * Semantics contract (shared with the JS mirror): a {@code null}, missing or non-numeric identifier
 * reads as {@code 0}; division by zero yields {@code 0}; rounding is half-up with ties away from
 * zero ({@link RoundingMode#HALF_UP}). Arithmetic is performed in <b>decimal</b> (#7578): addition,
 * subtraction and multiplication are exact, a quotient carries {@link MathContext#DECIMAL128} (34
 * significant digits), and only the final result is rounded to the property's scale. A
 * {@code decimal(18, 2)} column holds 18 significant digits and a {@code double} about 15, so a
 * double evaluation turned the largest value such a column holds into a longer one that overflowed
 * it on the insert, and below that ceiling silently changed the low cents. The JS mirror previews
 * in its own number type, so a preview at the very edge of a column's precision may differ in its
 * last digits from what is persisted; the persisted value is this one.
 */
public final class Calc {

    /**
     * The precision a quotient carries - enough that a division and its re-multiplication round back.
     */
    private static final MathContext QUOTIENT = MathContext.DECIMAL128;

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
        return new Parser(expression, entity).evaluate()
                                             .setScale(scale, RoundingMode.HALF_UP);
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

        private BigDecimal evaluate() {
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

        private BigDecimal parseExpr() {
            BigDecimal value = parseTerm();
            for (;;) {
                char c = peek();
                if (c == '+') {
                    pos++;
                    value = value.add(parseTerm());
                } else if (c == '-') {
                    pos++;
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            for (;;) {
                char c = peek();
                if (c == '*') {
                    pos++;
                    value = value.multiply(parseFactor());
                } else if (c == '/') {
                    pos++;
                    BigDecimal divisor = parseFactor();
                    value = divisor.signum() == 0 ? BigDecimal.ZERO : value.divide(divisor, QUOTIENT);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseFactor() {
            char c = peek();
            if (c == '-') {
                pos++;
                return parseFactor().negate();
            }
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            if (c == '(') {
                pos++;
                BigDecimal value = parseExpr();
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

        private BigDecimal parseNumber() {
            skipWhitespace();
            int start = pos;
            while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                pos++;
            }
            return decimal(source.substring(start, pos));
        }

        private BigDecimal parseIdentifierOrFunction() {
            skipWhitespace();
            int start = pos;
            while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                pos++;
            }
            String name = source.substring(start, pos);
            if (name.isEmpty()) {
                // Unrecognized character - skip it so a malformed expression yields 0 rather than looping.
                pos++;
                return BigDecimal.ZERO;
            }
            if (peek() == '(') {
                pos++;
                BigDecimal first = peek() == ')' ? BigDecimal.ZERO : parseExpr();
                BigDecimal second = BigDecimal.ZERO;
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

        private BigDecimal applyFunction(String name, BigDecimal a, BigDecimal b, boolean hasSecond) {
            switch (name) {
                case "round":
                    return a.setScale(hasSecond ? b.intValue() : 0, RoundingMode.HALF_UP);
                case "abs":
                    return a.abs();
                case "min":
                    return a.min(b);
                case "max":
                    return a.max(b);
                case "ceil":
                    return a.setScale(0, RoundingMode.CEILING);
                case "floor":
                    return a.setScale(0, RoundingMode.FLOOR);
                case "daysBetween":
                    return BigDecimal.valueOf(epochDay(b) - epochDay(a));
                case "businessDaysBetween":
                    return BigDecimal.valueOf(businessDaysBetween(epochDay(a), epochDay(b)));
                case "monthsBetween":
                    return BigDecimal.valueOf(monthsBetween(epochDay(a), epochDay(b)));
                default:
                    return BigDecimal.ZERO;
            }
        }

        /**
         * A date function's operand as a whole epoch day - the floor, as the date arithmetic always took
         * it.
         */
        private static long epochDay(BigDecimal value) {
            return value.setScale(0, RoundingMode.FLOOR)
                        .longValue();
        }

        /**
         * Mon-Fri dates in the closed interval of the two epoch days; {@code 0} when the interval is empty.
         * Counted arithmetically (no per-day loop) so a years-long span stays O(1): epoch day 0
         * (1970-01-01) was a Thursday, so {@code floorMod(epochDay + 3, 7)} is the weekday with Monday = 0
         * - the JS mirror derives the same index from {@code getUTCDay}.
         */
        private static long businessDaysBetween(long from, long to) {
            if (to < from) {
                return 0L;
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
        private static long monthsBetween(long from, long to) {
            java.time.LocalDate a = java.time.LocalDate.ofEpochDay(from);
            java.time.LocalDate b = java.time.LocalDate.ofEpochDay(to);
            return (b.getYear() - a.getYear()) * 12L + (b.getMonthValue() - a.getMonthValue());
        }

        /**
         * Read an identifier from the entity's public field - or, when the entity is a {@link Map}, from
         * the entry under that key, which is how the generated mapping code evaluates a formula over a
         * record that has no compiled type. Null / missing / non-numeric reads as 0. A date-typed value
         * reads as its epoch day so the date functions can consume it.
         */
        private BigDecimal readField(String name) {
            if (entity == null) {
                return BigDecimal.ZERO;
            }
            try {
                Object value;
                if (entity instanceof Map) {
                    value = ((Map<?, ?>) entity).get(name);
                } else {
                    Field field = entity.getClass()
                                        .getField(name);
                    value = field.get(entity);
                }
                if (value == null) {
                    return BigDecimal.ZERO;
                }
                if (value instanceof Number number) {
                    return decimal(number);
                }
                Long epochDay = toEpochDay(value);
                if (epochDay != null) {
                    return BigDecimal.valueOf(epochDay);
                }
                return decimal(value.toString()
                                    .trim());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return BigDecimal.ZERO;
            }
        }

        /**
         * A number as the decimal it stands for: a {@link BigDecimal} as it is - every digit of a money
         * column - and a floating-point value through its shortest decimal form, so {@code 0.1} reads as
         * {@code 0.1} rather than its binary expansion. A non-finite double reads as 0.
         */
        private static BigDecimal decimal(Number number) {
            if (number instanceof BigDecimal decimal) {
                return decimal;
            }
            if (number instanceof BigInteger integer) {
                return new BigDecimal(integer);
            }
            if (number instanceof Double || number instanceof Float) {
                double value = number.doubleValue();
                return Double.isFinite(value) ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(number.longValue());
        }

        /** A numeric literal or text, or 0 when it is empty or not a number. */
        private static BigDecimal decimal(String text) {
            if (text.isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        /**
         * The epoch day of a date-shaped value, or {@code null} when the value is not date-shaped. An ISO
         * {@code yyyy-MM-dd} prefix covers the string forms the HTML date/datetime inputs and the JSON
         * serializations produce - matching the JS mirror's coercion.
         */
        private static Long toEpochDay(Object value) {
            if (value instanceof java.time.LocalDate localDate) {
                return localDate.toEpochDay();
            }
            if (value instanceof java.time.LocalDateTime localDateTime) {
                return localDateTime.toLocalDate()
                                    .toEpochDay();
            }
            if (value instanceof java.time.Instant instant) {
                return instant.atZone(java.time.ZoneOffset.UTC)
                              .toLocalDate()
                              .toEpochDay();
            }
            if (value instanceof java.util.Date date) {
                // covers java.sql.Date / java.sql.Timestamp too
                return java.time.Instant.ofEpochMilli(date.getTime())
                                        .atZone(java.time.ZoneOffset.UTC)
                                        .toLocalDate()
                                        .toEpochDay();
            }
            if (value instanceof String text) {
                java.util.regex.Matcher m = ISO_DATE_PREFIX.matcher(text.trim());
                if (m.find()) {
                    try {
                        return java.time.LocalDate.parse(m.group())
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
