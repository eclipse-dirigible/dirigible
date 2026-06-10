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

import java.io.UnsupportedEncodingException;
import org.eclipse.dirigible.components.api.utils.UTF8Facade;

/**
 * UTF-8 byte / String conversion helpers. Equivalent to
 * {@code str.getBytes(StandardCharsets.UTF_8)} and
 * {@code new String(bytes, StandardCharsets.UTF_8)}; the wrapper exists so client code that reads
 * and writes through {@code byte[]} streams (e.g. {@link org.eclipse.dirigible.sdk.io.Streams
 * Streams} output) can keep encoding choices in one obvious place.
 * <p>
 * {@link #bytesToString(byte[], int, int)} lets callers decode a slice of a larger buffer without
 * an intermediate copy — handy when chunking I/O.
 */
public final class Utf8 {

    private Utf8() {}

    public static byte[] encode(String input) throws UnsupportedEncodingException {
        return UTF8Facade.encode(input);
    }

    public static String decode(byte[] input) {
        return UTF8Facade.decode(input);
    }

    public static String bytesToString(byte[] bytes, int offset, int length) throws UnsupportedEncodingException {
        return UTF8Facade.bytesToString(bytes, offset, length);
    }
}
