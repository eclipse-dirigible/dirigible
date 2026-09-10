/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The value vocabulary of a {@code posts:} {@code set:} entry - the forms the parser accepts and
 * the Java the generator renders, in one place so the two cannot drift.
 *
 * <p>
 * The vocabulary is closed on purpose. A value the renderer did not recognise used to be passed
 * through verbatim into the generated assignment, so an authored {@code Note: issued} - YAML having
 * stripped the quotes long before the renderer saw it - emitted {@code row.Note = issued;} and the
 * whole generated module stopped compiling (dirigible #7246). A plain constant is therefore
 * rendered as an escaped Java string literal, and a value that reads as an EXPRESSION the renderer
 * cannot compile is refused at parse time rather than turned into a string that would silently be
 * the wrong value.
 */
public final class PostSetSupport {

    /** A negated per-item copy: {@code -item.<Field>}. */
    private static final Pattern NEGATED_ITEM = Pattern.compile("^-\\s*item\\.(\\w+)$");

    /** A per-item copy: {@code item.<Field>}. */
    private static final Pattern ITEM = Pattern.compile("^item\\.(\\w+)$");

    /** A copy off the source record: {@code source.<Field>}. */
    private static final Pattern SOURCE = Pattern.compile("^source\\.(\\w+)$");

    /** A number - an integer (an FK id, a direction) or a decimal. */
    private static final Pattern NUMBER = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    /** A value the author quoted explicitly, to force the string reading of an expression-like text. */
    private static final Pattern QUOTED = Pattern.compile("^\"[^\"]*\"$");

    /**
     * Not instantiable.
     */
    private PostSetSupport() {}

    /**
     * Whether a value reads as an expression this renderer cannot compile - a dotted path off anything
     * but {@code item} / {@code source}, or a negation of anything but a per-item copy.
     *
     * <p>
     * Such a value is refused rather than rendered: the author meant a value read from somewhere, so
     * emitting the text as a string constant would put a wrong value in the ledger silently, which is
     * the one outcome worse than a refusal.
     *
     * @param raw the authored value
     * @return true when the value must be refused
     */
    public static boolean isUnsupportedExpression(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        if (NEGATED_ITEM.matcher(value)
                        .matches()
                || ITEM.matcher(value)
                       .matches()
                || SOURCE.matcher(value)
                         .matches()
                || NUMBER.matcher(value)
                         .matches()
                || QUOTED.matcher(value)
                         .matches()) {
            return false;
        }
        return value.indexOf('.') >= 0 || value.startsWith("-");
    }

    /**
     * The Java expression a {@code set:} value renders to, over the {@code source} record and - in a
     * {@code forEach} rule - the {@code item} row.
     *
     * <p>
     * The recognised forms are {@code item.<Field>}, {@code -item.<Field>} (null-safe),
     * {@code source.<Field>}, a number, {@code true} / {@code false} / {@code null}, and a value the
     * author quoted explicitly. Everything else is a plain constant and renders as an escaped Java
     * string literal - never as a bare identifier, which cannot compile.
     *
     * @param raw the authored value
     * @return the Java expression
     */
    public static String expression(String raw) {
        if (raw == null) {
            return "null";
        }
        String value = raw.trim();
        Matcher negated = NEGATED_ITEM.matcher(value);
        if (negated.matches()) {
            String access = "item." + IntentNaming.pascalCase(negated.group(1));
            return access + " == null ? null : " + access + ".negate()";
        }
        Matcher item = ITEM.matcher(value);
        if (item.matches()) {
            return "item." + IntentNaming.pascalCase(item.group(1));
        }
        Matcher source = SOURCE.matcher(value);
        if (source.matches()) {
            return "source." + IntentNaming.pascalCase(source.group(1));
        }
        if (NUMBER.matcher(value)
                  .matches()) {
            return value;
        }
        if ("true".equals(value) || "false".equals(value) || "null".equals(value)) {
            return value;
        }
        if (QUOTED.matcher(value)
                  .matches()) {
            return value;
        }
        return '"' + escape(value) + '"';
    }

    /**
     * Escapes a constant for placement inside a Java string literal: the backslash and the double quote
     * that would otherwise end the literal, and the control characters that would end the line.
     *
     * @param value the raw constant
     * @return the escaped constant, ready to be placed between two double quotes
     */
    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                default -> {
                    if (character < 0x20 || character == 0x7f) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
