/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import java.util.Locale;

/** How sure the model is about a value it read. */
public enum CaptureConfidence {
    /** Clearly legible and unambiguous. */
    HIGH,
    /** Legible, but its meaning is uncertain. */
    MEDIUM,
    /** Partly illegible or inferred. */
    LOW;

    /**
     * Parse a provider's confidence word, case-insensitively.
     *
     * @param value the word, may be null
     * @return the confidence; {@link #LOW} for an unknown or missing word
     */
    public static CaptureConfidence parse(String value) {
        if (value == null) {
            return LOW;
        }
        try {
            return valueOf(value.trim()
                                .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}
