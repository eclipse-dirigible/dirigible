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
 * The shapes an authored {@code defaultValue:} arrives in.
 *
 * <p>
 * One authored default is written into several generated languages - the repository's Java literal
 * ({@link JavaLiterals}) and the item dialog's JavaScript seed ({@link JsLiterals}) - and every one
 * of them has to read the authored text the same way, or the value the column holds and the value
 * the form offers stop agreeing.
 */
final class AuthoredDefaults {

    /**
     * Not instantiable.
     */
    private AuthoredDefaults() {}

    /**
     * Whether an authored boolean default reads as true.
     *
     * @param defaultValue the authored default
     * @return true when it does
     */
    static boolean readsAsTrue(String defaultValue) {
        return "true".equals(defaultValue) || "TRUE".equals(defaultValue) || "1".equals(defaultValue);
    }

    /**
     * Strips the SQL single quotes an authored string default may carry, leaving the string the column
     * would hold.
     *
     * <p>
     * Both authoring shapes are accepted - bare ({@code DRAFT}, what the item dialog seeds) and
     * SQL-quoted ({@code 'DRAFT'}, what a working DB DEFAULT needs, since the value reaches the DDL
     * verbatim).
     *
     * @param defaultValue the authored default
     * @return the value without its surrounding quotes
     */
    static String unquote(String defaultValue) {
        if (defaultValue.length() > 1 && defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
            return defaultValue.substring(1, defaultValue.length() - 1);
        }
        return defaultValue;
    }
}
