/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.util.Locale;

/**
 * The intent field's {@code normalize:} step (issue #7726): what a string value becomes before it
 * is validated and stored. A pasted {@code +359 898 123 456} is stored as {@code +359898123456}, so
 * a {@code pattern:} can demand the canonical form without refusing the way people actually type
 * it.
 *
 * <p>
 * The generated repository applies it on every create and update, and the generated controllers
 * before their checks, so the value a {@code pattern:} or a {@code checks:} rule reads is the value
 * that is stored. The transforms run in one fixed order whatever order the intent lists them in:
 * trim, then strip, then the case fold.
 */
public final class Normalize {

    private Normalize() {}

    /**
     * Normalizes one value.
     *
     * @param value the submitted value, may be null
     * @param trim whether leading and trailing whitespace is removed
     * @param strip the characters removed wherever they occur, null or empty for none
     * @param caseFold {@code upper}, {@code lower}, or null for none
     * @return the normalized value, or null for a null value
     */
    public static String apply(String value, boolean trim, String strip, String caseFold) {
        if (value == null) {
            return null;
        }
        String result = trim ? value.trim() : value;
        if (strip != null && !strip.isEmpty()) {
            StringBuilder kept = new StringBuilder(result.length());
            result.codePoints()
                  .filter(codePoint -> strip.indexOf(codePoint) < 0)
                  .forEach(kept::appendCodePoint);
            result = kept.toString();
        }
        if ("upper".equals(caseFold)) {
            result = result.toUpperCase(Locale.ROOT);
        } else if ("lower".equals(caseFold)) {
            result = result.toLowerCase(Locale.ROOT);
        }
        return result;
    }
}
