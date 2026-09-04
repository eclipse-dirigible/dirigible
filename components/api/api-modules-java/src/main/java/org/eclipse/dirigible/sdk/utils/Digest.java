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
import org.eclipse.dirigible.components.api.utils.DigestFacade;

/**
 * One-shot cryptographic-digest helpers — MD5, SHA-1, SHA-256, SHA-384, SHA-512. Every algorithm
 * accepts either {@code byte[]} or {@link String} input; MD5 and SHA-1 additionally offer a
 * {@code Hex} variant returning a printable {@link String} instead of the raw {@code byte[]}. Use
 * the {@code Hex} variants when you want a printable identifier (cache keys, ETags, fingerprints)
 * and the raw byte variants when you'll feed the digest into another cryptographic step; wrap any
 * digest in {@link Hex#encode(byte[])} to print it.
 * <p>
 * A {@link String} argument is always text — it is digested as its UTF-8 bytes, so
 * {@code sha256(text)} equals {@code sha256(text.getBytes(StandardCharsets.UTF_8))}.
 * <p>
 * MD5 and SHA-1 are kept for compatibility with file fingerprinting / ETag use cases but should not
 * be used for any new security-sensitive purpose — prefer {@link #sha256(String) sha256} or
 * {@link #sha512(String) sha512}. For password hashing use a dedicated PBKDF2 / bcrypt / Argon2
 * library, not these helpers.
 */
public final class Digest {

    private Digest() {}

    /**
     * Calculates the MD5 digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the digest
     */
    public static byte[] md5(String input) {
        return DigestFacade.md5(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] md5(byte[] input) {
        return DigestFacade.md5(input);
    }

    /**
     * Calculates the MD5 hex-encoded digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the hex-encoded digest
     */
    public static String md5Hex(String input) {
        return DigestFacade.md5Hex(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String md5Hex(byte[] input) {
        return DigestFacade.md5Hex(input);
    }

    /**
     * Calculates the SHA-1 digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the digest
     */
    public static byte[] sha1(String input) {
        return DigestFacade.sha1(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha1(byte[] input) {
        return DigestFacade.sha1(input);
    }

    /**
     * Calculates the SHA-1 hex-encoded digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the hex-encoded digest
     */
    public static String sha1Hex(String input) {
        return DigestFacade.sha1Hex(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha1Hex(byte[] input) {
        return DigestFacade.sha1Hex(input);
    }

    /**
     * Calculates the SHA-256 digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the digest
     */
    public static byte[] sha256(String input) {
        return DigestFacade.sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha256(byte[] input) {
        return DigestFacade.sha256(input);
    }

    /**
     * Calculates the SHA-384 digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the digest
     */
    public static byte[] sha384(String input) {
        return DigestFacade.sha384(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha384(byte[] input) {
        return DigestFacade.sha384(input);
    }

    /**
     * Calculates the SHA-512 digest of the text's UTF-8 bytes.
     *
     * @param input the text to digest
     * @return the digest
     */
    public static byte[] sha512(String input) {
        return DigestFacade.sha512(input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] sha512(byte[] input) {
        return DigestFacade.sha512(input);
    }
}
