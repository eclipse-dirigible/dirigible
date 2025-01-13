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

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.springframework.stereotype.Component;

/**
 * The Class DigestFacade.
 */
@Component
public class DigestFacade {

    /**
     * Calculates the MD5 digest and returns the value as a 16 element byte[].
     *
     * @param input the input
     * @return the MD5 digest
     */
    public static final byte[] md5(byte[] input) {
        return DigestUtils.md5(input);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 16 element byte[].
     *
     * @param input the input
     * @return the MD5 digest
     */
    public static final byte[] md5(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return md5(bytes);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param input the input
     * @return the MD5 digest
     */
    public static final String md5Hex(byte[] input) {
        return DigestUtils.md5Hex(input);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param input the input
     * @return the MD5 digest
     */
    public static final String md5Hex(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return md5Hex(bytes);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-1 digest
     */
    public static final byte[] sha1(byte[] input) {
        return DigestUtils.sha1(input);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-1 digest
     */
    public static final byte[] sha1(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return sha1(bytes);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-256 digest
     */
    public static final byte[] sha256(byte[] input) {
        return DigestUtils.sha256(input);
    }

    /**
     * Calculates the SHA-256 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-256 digest
     */
    public static final byte[] sha256(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return sha256(bytes);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-384 digest
     */
    public static final byte[] sha384(byte[] input) {
        return DigestUtils.sha384(input);
    }

    /**
     * Calculates the SHA-384 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-384 digest
     */
    public static final byte[] sha384(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return sha384(bytes);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-512 digest
     */
    public static final byte[] sha512(byte[] input) {
        return DigestUtils.sha512(input);
    }

    /**
     * Calculates the SHA-512 digest and returns the value as a byte[].
     *
     * @param input the input
     * @return the SHA-512 digest
     */
    public static final byte[] sha512(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return sha512(bytes);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hex string.
     *
     * @param input the input
     * @return the SHA-1 digest
     */
    public static final String sha1Hex(byte[] input) {
        return DigestUtils.sha1Hex(input);
    }

    /**
     * Calculates the SHA-1 digest and returns the value as a hex string.
     *
     * @param input the input
     * @return the SHA-1 digest
     */
    public static final String sha1Hex(String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        return sha1Hex(bytes);
    }

}
