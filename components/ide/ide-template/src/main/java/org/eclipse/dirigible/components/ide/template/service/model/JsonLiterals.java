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
 * JSON literals for values a model carries as text.
 *
 * <p>
 * The generated {@code .schema} is a JSON document assembled by a template, which interpolates a
 * model value verbatim. A value holding a quote or a backslash therefore used to end the string it
 * was being written into and leave the whole artefact unparseable - an authored
 * {@code defaultValue: '6"'} rendered {@code "defaultValue": "6"",} and the synchronizer then
 * created no table for ANY entity of the project (dirigible #7206). Escaping the value keeps the
 * worst case at one mis-valued column DEFAULT.
 *
 * <p>
 * The twin of {@link JavaLiterals} for the other literal syntax the same authored value reaches.
 */
final class JsonLiterals {

    /**
     * Not instantiable.
     */
    private JsonLiterals() {}

    /**
     * Escapes a value for placement inside a JSON string: the backslash and the double quote that would
     * otherwise end the string, and the control characters JSON does not admit in one at all.
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
                    if (character < 0x20) {
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
     * A value as a complete JSON string literal, quotes included.
     *
     * <p>
     * The quotes belong to the literal rather than to the template around it, so that a template can
     * read the key's presence as "there is a value here" and never has to write a quote of its own next
     * to an interpolation.
     *
     * @param value the raw value
     * @return the quoted, escaped literal
     */
    static String stringLiteral(String value) {
        return "\"" + escape(value) + "\"";
    }
}
