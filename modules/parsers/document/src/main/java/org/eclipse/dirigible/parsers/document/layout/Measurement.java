/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.layout;

/**
 * A parsed width/height/gap value of the document DSL. The supported forms are {@code 100} (pixels
 * — a bare number is absolute), {@code 100px}, {@code 50%}, {@code *} (fraction weight 1), and
 * {@code 2*} (fraction weight 2); a missing or {@code auto} value is {@link #AUTO}.
 *
 * @param type the measurement kind
 * @param value the numeric amount; {@code 0} for {@link Type#AUTO}
 */
public record Measurement(Type type, double value) {

    /** The measurement kinds. */
    public enum Type {
        /** An absolute length in pixels ({@code 100}, {@code 100px}). */
        ABSOLUTE_PX,
        /** A percentage of the enclosing space ({@code 50%}). */
        PERCENT,
        /** A weight in the space left after absolute and percent siblings ({@code *}, {@code 2*}). */
        FRACTION,
        /** Size to content — the default when no value is given. */
        AUTO
    }

    /** The shared AUTO measurement. */
    public static final Measurement AUTO = new Measurement(Type.AUTO, 0);

    /**
     * Validates the amount.
     *
     * @param type the measurement kind
     * @param value the numeric amount
     */
    public Measurement {
        if (value < 0) {
            throw new IllegalArgumentException("Measurement value must not be negative: " + value);
        }
    }

    /**
     * Parses a raw attribute value.
     *
     * @param raw the value as authored, e.g. {@code "50%"}; {@code null}, blank and {@code "auto"}
     *        parse to {@link #AUTO}
     * @return the measurement
     * @throws IllegalArgumentException when the value is not a valid measurement
     */
    public static Measurement parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase("auto")) {
            return AUTO;
        }
        try {
            if (value.equals("*")) {
                return fraction(1);
            }
            if (value.endsWith("*")) {
                return fraction(parseAmount(value.substring(0, value.length() - 1)));
            }
            if (value.endsWith("%")) {
                return percent(parseAmount(value.substring(0, value.length() - 1)));
            }
            if (value.endsWith("px")) {
                return px(parseAmount(value.substring(0, value.length() - 2)));
            }
            return px(parseAmount(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid measurement: '" + raw + "'");
        }
    }

    private static double parseAmount(String amount) {
        String trimmed = amount.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Missing amount");
        }
        // strictly digits with at most one decimal point — Double.parseDouble alone would also
        // accept scientific notation, hex floats and type suffixes like "1d"
        boolean dotSeen = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '.') {
                if (dotSeen || trimmed.length() == 1) {
                    throw new IllegalArgumentException("Invalid amount");
                }
                dotSeen = true;
            } else if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid amount");
            }
        }
        return Double.parseDouble(trimmed);
    }

    /**
     * An absolute pixel measurement.
     *
     * @param value the length in pixels
     * @return the measurement
     */
    public static Measurement px(double value) {
        return new Measurement(Type.ABSOLUTE_PX, value);
    }

    /**
     * A percentage measurement.
     *
     * @param value the percentage
     * @return the measurement
     */
    public static Measurement percent(double value) {
        return new Measurement(Type.PERCENT, value);
    }

    /**
     * A fraction-weight measurement.
     *
     * @param value the weight
     * @return the measurement
     */
    public static Measurement fraction(double value) {
        return new Measurement(Type.FRACTION, value);
    }
}
