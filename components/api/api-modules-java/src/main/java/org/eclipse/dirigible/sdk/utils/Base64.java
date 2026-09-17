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

import java.nio.charset.StandardCharsets;
import org.eclipse.dirigible.components.api.utils.Base64Facade;

/**
 * Base64 encode/decode helpers in the standard (non URL-safe) alphabet. The {@code encode} and
 * {@code decode} overloads handle the common String &harr; byte[] cases; the {@code Native} pair
 * returns / accepts byte arrays on both sides for callers that already work in bytes (avoiding the
 * intermediate {@link String} allocation).
 * <p>
 * A {@link String} argument is always text: {@link #encode(String)} encodes its UTF-8 bytes and
 * {@link #decode(String)} reads base64 text, so {@code decode(encode(text))} round-trips.
 * <p>
 * Prefer this over {@link java.util.Base64} when you want behaviour identical to the TS / JS
 * surface — the underlying facade uses Apache Commons Codec, which produces unchunked output (no
 * embedded line breaks).
 */
public final class Base64 {

    private Base64() {}

    /**
     * Base64-encodes the UTF-8 bytes of the given text.
     *
     * @param input the text to encode
     * @return the base64 representation
     */
    public static String encode(String input) {
        return Base64Facade.encode(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(byte[] input) {
        return Base64Facade.encode(input);
    }

    public static byte[] decode(String input) {
        return Base64Facade.decode(input);
    }

    public static byte[] decodeNative(byte[] input) {
        return Base64Facade.decodeNative(input);
    }

    public static byte[] encodeNative(byte[] input) {
        return Base64Facade.encodeNative(input);
    }
}
