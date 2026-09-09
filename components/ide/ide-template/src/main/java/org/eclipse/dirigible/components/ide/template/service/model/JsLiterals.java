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

import java.util.regex.Pattern;

/**
 * JavaScript literals for values a model carries as text.
 *
 * <p>
 * The Java twin of this ({@link JavaLiterals}) exists for the same reason, one language over: a
 * model value reaches a generated source through a template, which interpolates it verbatim, so a
 * value holding the quote that delimits the literal used to end it and break the whole generated
 * file. An authored {@code defaultValue: "Owner's copy"} rendered {@code def: 'Owner's copy'} and
 * made the register a syntax error, so the page failed to load entirely rather than one field
 * mis-seeding (dirigible #7207).
 */
final class JsLiterals {

    /**
     * The authored texts that may stand unquoted where a number is expected. Deliberately narrower than
     * JavaScript's numeric grammar - anything else falls back to a string literal, which mis-seeds one
     * field instead of ending the literal.
     */
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    /**
     * Not instantiable.
     */
    private JsLiterals() {}

    /**
     * Escapes a value for placement inside a single-quoted JavaScript string literal: the backslash and
     * the apostrophe that would otherwise end the literal, and the control characters that would end
     * the line.
     *
     * @param value the raw value
     * @return the escaped value, ready to be placed between two apostrophes
     */
    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '\'' -> escaped.append("\\'");
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
     * The authored default of a property as the JavaScript value the item dialog seeds a new line with,
     * or null when the property has no default to seed.
     *
     * <p>
     * The value is emitted in the shape the draft holds: a checkbox takes a real boolean, a numeric
     * column a real number, and everything else a string - including a dropdown's foreign key, which
     * the draft keeps stringified so it matches an option's data-value. A numeric default that is not a
     * number falls back to a string, so an author's typo mis-seeds that one field instead of making the
     * whole register a syntax error.
     *
     * @param widgetType the property's widget type
     * @param numeric whether the property renders as a number
     * @param defaultValue the authored default, as the model carries it
     * @return the JavaScript expression, or null when there is none
     */
    static String defaultValueExpression(String widgetType, boolean numeric, String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }
        if ("CHECKBOX".equals(widgetType)) {
            return AuthoredDefaults.readsAsTrue(defaultValue) ? "true" : "false";
        }
        String value = AuthoredDefaults.unquote(defaultValue);
        if (numeric && !"DROPDOWN".equals(widgetType) && NUMBER.matcher(value)
                                                               .matches()) {
            return value;
        }
        return "'" + escape(value) + "'";
    }
}
