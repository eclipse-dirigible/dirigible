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

/**
 * Java literals for values a model carries as text.
 *
 * <p>
 * A model value reaches a generated Java source through a template, which interpolates it verbatim.
 * A value holding a quote or a backslash therefore used to end the literal it was being written
 * into and break the compile of the whole generated module - an authored {@code defaultValue: '6"'}
 * rendered {@code entity.Size = "6"";} (dirigible #7154). Escaping the value keeps the worst case
 * at one mis-valued field.
 */
final class JavaLiterals {

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
    static String escape(String value) {
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
     * A numeric default is parsed from its authored text rather than inlined as a numeric literal, so
     * an author's {@code "8.0"} on an integer column fails that one create instead of failing the whole
     * generated build. A string default is read in either authoring shape ({@link AuthoredDefaults}),
     * and both yield the string the column would hold. A date/time or binary column has no literal: its
     * DEFAULT is emitted verbatim into the DDL and is typically a SQL expression ({@code CURRENT_DATE},
     * {@code now()}).
     *
     * @param javaClass the property's Java class, as the parameter graph resolved it
     * @param defaultValue the authored default, as the model carries it
     * @return the Java expression, or null when there is none
     */
    static String defaultValueExpression(String javaClass, String defaultValue) {
        if (javaClass == null || defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        return switch (javaClass) {
            case "java.math.BigDecimal" -> "new java.math.BigDecimal(\"" + escape(defaultValue) + "\")";
            case "Double" -> "Double.valueOf(\"" + escape(defaultValue) + "\")";
            case "Float" -> "Float.valueOf(\"" + escape(defaultValue) + "\")";
            case "Long" -> "Long.valueOf(\"" + escape(defaultValue) + "\")";
            case "Integer" -> "Integer.valueOf(\"" + escape(defaultValue) + "\")";
            case "Short" -> "Short.valueOf(\"" + escape(defaultValue) + "\")";
            case "Boolean" -> AuthoredDefaults.readsAsTrue(defaultValue) ? "Boolean.TRUE" : "Boolean.FALSE";
            case "String" -> "\"" + escape(AuthoredDefaults.unquote(defaultValue)) + "\"";
            default -> null;
        };
    }
}
