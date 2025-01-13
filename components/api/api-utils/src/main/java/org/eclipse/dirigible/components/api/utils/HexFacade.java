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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.springframework.stereotype.Component;

/**
 * The Class HexFacade.
 */
@Component
public class HexFacade {

    /**
     * Converts an array of bytes into a String representing the hexadecimal values of each byte in
     * order. The returned String will be double the length of the passed array, as it takes two
     * characters to represent any given byte.
     *
     * @param input the input byte array
     * @return the hexadecimal value
     */
    public static final String encode(byte[] input) {
        return Hex.encodeHexString(input);
    }

    /**
     * Converts an array of bytes into a String representing the hexadecimal values of each byte in
     * order. The returned String will be double the length of the passed array, as it takes two
     * characters to represent any given byte.
     *
     * @param input the input string
     * @return the hexadecimal value
     */
    public static final String encode(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return encode(bytes);
    }

    /**
     * Hex encode.
     *
     * @param input the input
     * @return the hex encoded input
     */
    public static final byte[] encodeNative(byte[] input) {
        Hex hex = new Hex();
        return hex.encode(input);
    }

    /**
     * Converts an array of characters representing hexadecimal values into an array of bytes of those
     * same values. The returned array will be half the length of the passed array, as it takes two
     * characters to represent any given byte. An exception is thrown if the passed char array has an
     * odd number of elements.
     *
     * @param input the input string
     * @return the input decoded
     * @throws DecoderException in case of decoding failure
     */
    public static final byte[] decode(String input) throws DecoderException {
        return Hex.decodeHex(input.toCharArray());
    }

    /**
     * Hex decode.
     *
     * @param input the input
     * @return the hex decoded output
     * @throws DecoderException the decoder exception
     */
    public static final byte[] decodeNative(byte[] input) throws DecoderException {
        Hex hex = new Hex();
        return hex.decode(input);
    }

}
