/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.renderer;

import java.util.Locale;

/**
 * The error-correction level of a {@code <qrcode>} - how much of the printed symbol may be damaged
 * or covered and still scan: about 7% ({@link #L}), 15% ({@link #M}), 25% ({@link #Q}) or 30%
 * ({@link #H}). A higher level makes a denser symbol for the same payload.
 */
public enum QrCodeLevel {

    /** Low, about 7%. */
    L,
    /** Medium, about 15% - the default, and the level the EPC069-12 payment code prescribes. */
    M,
    /** Quartile, about 25%. */
    Q,
    /** High, about 30%. */
    H;

    /**
     * Parses a {@code level} attribute value.
     *
     * @param value the raw value, case-insensitive; {@code null} or blank means {@link #M}
     * @return the level
     * @throws IllegalArgumentException when the value names no level
     */
    public static QrCodeLevel parse(String value) {
        if (value == null || value.isBlank()) {
            return M;
        }
        try {
            return valueOf(value.trim()
                                .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid level: '" + value + "' (expected L, M, Q or H)");
        }
    }
}
