/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import java.security.SecureRandom;

/**
 * Random string generators suitable for tokens, short identifiers, and one-shot secrets that fit a
 * "human readable" requirement (no symbols, no ambiguity-prone glyphs are filtered out — call out
 * to a dedicated library for that). The implementation uses {@link SecureRandom}, so the outputs
 * are cryptographically random; cost is comparable to one {@code nextInt} per character.
 * <p>
 * Use {@link #random(int, String)} when you need to constrain the alphabet (e.g. uppercase-only for
 * case-insensitive shortcodes), and {@link #randomNumeric(int)} when only digits are wanted.
 */
public final class Alphanumeric {

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NUMERIC = "0123456789";
    private static final String ALPHANUMERIC = ALPHA + NUMERIC;
    private static final int DEFAULT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Alphanumeric() {}

    public static String random() {
        return random(DEFAULT_LENGTH, ALPHANUMERIC);
    }

    public static String random(int length) {
        return random(length, ALPHANUMERIC);
    }

    public static String random(int length, String charset) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        if (charset == null || charset.isEmpty()) {
            throw new IllegalArgumentException("charset must be non-empty");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }

    public static String randomAlpha() {
        return random(DEFAULT_LENGTH, ALPHA);
    }

    public static String randomAlpha(int length) {
        return random(length, ALPHA);
    }

    public static String randomNumeric() {
        return random(DEFAULT_LENGTH, NUMERIC);
    }

    public static String randomNumeric(int length) {
        return random(length, NUMERIC);
    }
}
