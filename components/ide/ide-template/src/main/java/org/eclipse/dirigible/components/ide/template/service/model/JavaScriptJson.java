/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import java.util.List;
import java.util.Map;

/**
 * Serializes a parameter graph the way {@code JSON.stringify} does.
 *
 * <p>
 * The generation pipeline writes two JSON artefacts that are content-compared across regenerations
 * - the {@code .gen} audit descriptor and the translation catalogs - so their formatting is part of
 * the contract rather than an implementation detail. Gson differs from {@code JSON.stringify} in
 * three ways that all show up in those files: it drops null-valued keys, it escapes HTML characters
 * as unicode sequences, and it writes an integral double as {@code 6.0} where JavaScript writes
 * {@code 6}. Rather than configure around the first two and still lose on the third, this writes
 * the JavaScript form directly.
 *
 * <p>
 * Numbers outside the range where a double is exactly integral fall back to Java's own formatting;
 * the exponent forms JavaScript would produce for extreme magnitudes are not reachable from a model
 * file.
 */
final class JavaScriptJson {

    /** The indentation of one nesting level, matching the pipeline's two-space output. */
    private static final String INDENT = "  ";

    /**
     * The magnitude below which a double is written in integer form. Well inside the exactly-integral
     * range of both a double and a long, so that form can never lose information.
     */
    private static final double MAX_INTEGRAL = 1e15;

    /**
     * Not instantiable.
     */
    private JavaScriptJson() {}

    /**
     * Serializes a value with two-space indentation, for a file a human reads and a diff compares.
     *
     * @param value the value
     * @return the JSON text
     */
    static String pretty(Object value) {
        StringBuilder out = new StringBuilder(256);
        write(out, value, 0, true);
        return out.toString();
    }

    /**
     * Serializes a value without whitespace, for a value embedded into generated code.
     *
     * @param value the value
     * @return the JSON text
     */
    static String compact(Object value) {
        StringBuilder out = new StringBuilder(256);
        write(out, value, 0, false);
        return out.toString();
    }

    /**
     * Writes one value.
     *
     * @param out the target
     * @param value the value
     * @param depth the current nesting depth
     * @param pretty whether to indent
     */
    private static void write(StringBuilder out, Object value, int depth, boolean pretty) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            writeString(out, string);
        } else if (value instanceof Boolean bool) {
            out.append(bool.booleanValue() ? "true" : "false");
        } else if (value instanceof Number number) {
            writeNumber(out, number);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(out, map, depth, pretty);
        } else if (value instanceof List<?> list) {
            writeArray(out, list, depth, pretty);
        } else {
            writeString(out, value.toString());
        }
    }

    /**
     * Writes an object in insertion order.
     *
     * @param out the target
     * @param map the object
     * @param depth the current nesting depth
     * @param pretty whether to indent
     */
    private static void writeObject(StringBuilder out, Map<?, ?> map, int depth, boolean pretty) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newLine(out, depth + 1, pretty);
            writeString(out, String.valueOf(entry.getKey()));
            out.append(':');
            if (pretty) {
                out.append(' ');
            }
            write(out, entry.getValue(), depth + 1, pretty);
        }
        newLine(out, depth, pretty);
        out.append('}');
    }

    /**
     * Writes an array.
     *
     * @param out the target
     * @param list the array
     * @param depth the current nesting depth
     * @param pretty whether to indent
     */
    private static void writeArray(StringBuilder out, List<?> list, int depth, boolean pretty) {
        if (list.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        boolean first = true;
        for (Object element : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newLine(out, depth + 1, pretty);
            write(out, element, depth + 1, pretty);
        }
        newLine(out, depth, pretty);
        out.append(']');
    }

    /**
     * Breaks the line and indents to a nesting level, when indenting at all.
     *
     * @param out the target
     * @param depth the depth
     * @param pretty whether to indent
     */
    private static void newLine(StringBuilder out, int depth, boolean pretty) {
        if (pretty) {
            out.append('\n')
               .append(INDENT.repeat(depth));
        }
    }

    /**
     * Writes a number, dropping the fractional part of an integral value the way JavaScript does.
     *
     * @param out the target
     * @param number the number
     */
    private static void writeNumber(StringBuilder out, Number number) {
        double value = number.doubleValue();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            // JavaScript has no literal for either, and serializes both as null.
            out.append("null");
            return;
        }
        if (value == Math.floor(value) && Math.abs(value) < MAX_INTEGRAL) {
            // Math.round rather than a (long) cast: it says "the integral value" instead of relying on
            // narrowing, and it saturates instead of wrapping should the guard above ever be relaxed.
            // For an integral magnitude under MAX_INTEGRAL the two agree exactly.
            out.append(Math.round(value));
            return;
        }
        out.append(number instanceof Float ? Float.toString(number.floatValue()) : Double.toString(value));
    }

    /**
     * Writes a quoted string, escaping exactly what JavaScript escapes.
     *
     * @param out the target
     * @param value the string
     */
    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

}
