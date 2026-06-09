/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.io;

import java.io.UnsupportedEncodingException;
import org.eclipse.dirigible.components.api.io.BytesFacade;

/**
 * Byte-buffer conversions that don't fit cleanly into a single JDK call — text-to-bytes with a
 * named charset and integer / byte-array conversion with explicit byte order. The byte-order
 * constants {@link #BIG_ENDIAN} and {@link #LITTLE_ENDIAN} match the values the underlying facade
 * accepts; use them as the {@code byteOrder} argument rather than passing raw strings.
 * <p>
 * For straight ASCII / UTF-8 conversion you can use {@link org.eclipse.dirigible.sdk.utils.Utf8
 * Utf8} or the standard library directly; this class earns its keep when you actually need a
 * specific charset or a known endianness.
 */
public final class Bytes {

    public static final String BIG_ENDIAN = "BIG_ENDIAN";
    public static final String LITTLE_ENDIAN = "LITTLE_ENDIAN";

    private Bytes() {}

    public static byte[] textToByteArray(String text) {
        return BytesFacade.textToByteArray(text);
    }

    public static byte[] textToByteArray(String text, String charset) throws UnsupportedEncodingException {
        return BytesFacade.textToByteArray(text, charset);
    }

    public static byte[] intToByteArray(int value, String byteOrder) {
        return BytesFacade.intToByteArray(value, byteOrder);
    }

    public static int byteArrayToInt(byte[] data, String byteOrder) {
        return BytesFacade.byteArrayToInt(data, byteOrder);
    }
}
