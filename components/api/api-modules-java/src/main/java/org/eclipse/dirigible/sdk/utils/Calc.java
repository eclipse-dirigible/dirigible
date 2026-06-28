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
 * {@code max(a, b)}, {@code ceil(x)} and {@code floor(x)}.
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
                default:
                    return 0d;
            }
        }

        /** Read an identifier from the entity's public field; null / missing / non-numeric reads as 0. */
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
    }
}
