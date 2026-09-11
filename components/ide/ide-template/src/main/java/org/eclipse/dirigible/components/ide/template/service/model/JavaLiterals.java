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

import java.util.function.Consumer;

/**
 * Java literals for values a model carries as text.
 *
 * <p>
 * A model value reaches a generated Java source through a template, which interpolates it verbatim.
 * A value holding a quote or a backslash therefore used to end the literal it was being written
 * into and break the compile of the whole generated module - an authored {@code defaultValue: '6"'}
 * rendered {@code entity.Size = "6"";} (dirigible #7154). Escaping the value keeps the worst case
 * at one mis-valued field.
 *
 * <p>
 * {@link #escape(String)} is THE escape for "an authored value inside a generated Java literal",
 * and it is public for that reason: the loop was copied into engine-intent twice before (dirigible
 * #7287), where a copy that drifts is a compile error in a generated module nobody sees until a
 * regen. Anything that renders a Java literal from a model value calls this - it does not grow a
 * fourth copy.
 */
public final class JavaLiterals {

    /**
     * Not instantiable.
     */
    private JavaLiterals() {}

    /**
     * Escapes a value for placement inside a Java string literal: the backslash and the double quote
     * that would otherwise end the literal, and the control characters that would end the line.
     *
     * @param value the raw value
     * @return the escaped value, ready to be placed between two double quotes
     */
    public static String escape(String value) {
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

    /**
     * The authored default of a property as a Java expression of the property's own type, or null when
     * the property has no default that a Java literal can stand in for.
     *
     * <p>
     * Every arm reads the authored text through {@link AuthoredDefaults}, so both authoring shapes -
     * bare and SQL-quoted - yield the value the column would hold. Reading it in only one arm is how
     * {@code defaultValue: "'20'"} on an integer column seeded {@code 20} in the item dialog and
     * emitted {@code Integer.valueOf("'20'")} in the repository, a {@code NumberFormatException} on
     * every create that relied on the default (dirigible #7293).
     *
     * <p>
     * A numeric default is parsed from its text rather than inlined as a numeric literal - the same
     * parse the generated expression performs, which is why it is run HERE: an unparsable numeric
     * default is refused while the author is generating, naming the property, instead of compiling into
     * an expression that throws on every create of that entity. A date/time or binary column has no
     * literal: its DEFAULT is emitted verbatim into the DDL and is typically a SQL expression
     * ({@code CURRENT_DATE}, {@code now()}).
     *
     * @param javaClass the property's Java class, as the parameter graph resolved it
     * @param defaultValue the authored default, as the model carries it
     * @param property the property the default is authored on, for the refusal message
     * @return the Java expression, or null when there is none
     * @throws IllegalArgumentException when a numeric property's default is not a value of its type
     */
    public static String defaultValueExpression(String javaClass, String defaultValue, String property) {
        if (javaClass == null || defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        String value = AuthoredDefaults.unquote(defaultValue);
        return switch (javaClass) {
            case "java.math.BigDecimal" -> numericExpression("new java.math.BigDecimal", value, javaClass, property,
                    java.math.BigDecimal::new);
            case "Double" -> numericExpression("Double.valueOf", value, javaClass, property, Double::valueOf);
            case "Float" -> numericExpression("Float.valueOf", value, javaClass, property, Float::valueOf);
            case "Long" -> numericExpression("Long.valueOf", value, javaClass, property, Long::valueOf);
            case "Integer" -> numericExpression("Integer.valueOf", value, javaClass, property, Integer::valueOf);
            case "Short" -> numericExpression("Short.valueOf", value, javaClass, property, Short::valueOf);
            case "Boolean" -> AuthoredDefaults.readsAsTrue(defaultValue) ? "Boolean.TRUE" : "Boolean.FALSE";
            case "String" -> "\"" + escape(value) + "\"";
            default -> null;
        };
    }

    /**
     * A numeric default as the factory call the generated code applies it through, refusing a text the
     * very same factory cannot read.
     *
     * @param factory the factory the expression calls
     * @param value the authored default, unquoted
     * @param javaClass the property's Java class, for the refusal message
     * @param property the property the default is authored on, for the refusal message
     * @param parse the factory itself, run here on the authored text
     * @return the factory call
     */
    private static String numericExpression(String factory, String value, String javaClass, String property, Consumer<String> parse) {
        try {
            parse.accept(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Property [" + property + "] declares the default [" + value
                    + "], which is not a value of its type [" + javaClass + "] - every create applying it would fail.", ex);
        }
        return factory + "(\"" + escape(value) + "\")";
    }
}
