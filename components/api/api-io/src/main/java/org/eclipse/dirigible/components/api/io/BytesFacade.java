/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.springframework.stereotype.Component;

/**
 * The Class BytesFacade.
 */
@Component
public class BytesFacade {

    /**
     * Text to byte array.
     *
     * @param text the text
     * @return the byte[]
     */
    public static byte[] textToByteArray(String text) {
        byte[] bytes = text.getBytes();
        return bytes;
    }

    /**
     * Text to byte array.
     *
     * @param text the text
     * @param charset the charset
     * @return the byte[]
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static byte[] textToByteArray(String text, String charset) throws UnsupportedEncodingException {
        byte[] bytes = text.getBytes(charset);
        return bytes;
    }

    /**
     * Int to byte array.
     *
     * @param value the value
     * @param byteOrder the byte order
     * @return the byte[]
     */
    public static byte[] intToByteArray(int value, String byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        ByteOrder order = byteOrder.equals(ByteOrder.BIG_ENDIAN.toString()) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        buffer.order(order);
        buffer.putInt(value);
        return buffer.array();
    }

    /**
     * Byte array to int.
     *
     * @param data the data
     * @param byteOrder the byte order
     * @return the int
     */
    public static int byteArrayToInt(byte[] data, String byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        ByteOrder order = byteOrder.equals(ByteOrder.BIG_ENDIAN.toString()) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        buffer.order(order);
        buffer.put(data);
        return buffer.getInt(0);
    }


}
