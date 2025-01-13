/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;


/**
 * The Class UTF8Facade.
 */
@Component
public class UTF8Facade {

    /**
     * UTF8 encode.
     *
     * @param input the input
     * @return the utf8encoded input
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static final byte[] encode(String input) throws UnsupportedEncodingException {
        return input.getBytes(StandardCharsets.UTF_8);
    }



    /**
     * UTF8 decode.
     *
     * @param input the input
     * @return the utf8 decoded output
     */
    public static final String decode(byte[] input) {
        return new String(input, StandardCharsets.UTF_8);
    }


    /**
     * UTF8 bytes to string.
     *
     * @param bytes the input byte array
     * @param offset the byte offset
     * @param length the length of the byte array
     * @return the utf8 decoded output
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static final String bytesToString(byte[] bytes, int offset, int length) throws UnsupportedEncodingException {
        return new String(bytes, offset, length, "UTF-8");
    }


}
