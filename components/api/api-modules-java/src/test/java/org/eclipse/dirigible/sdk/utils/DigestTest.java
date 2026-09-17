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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Every {@code (String)} digest overload hashes the text's UTF-8 bytes. The expected values are the
 * published digests of "hello", so a change of input interpretation cannot pass unnoticed.
 */
class DigestTest {

    private static final String INPUT = "hello";

    private static byte[] utf8() {
        return INPUT.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void md5HexOfText() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", Digest.md5Hex(INPUT));
    }

    @Test
    void md5OfText() {
        assertArrayEquals(Digest.md5(utf8()), Digest.md5(INPUT));
    }

    @Test
    void sha1HexOfText() {
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", Digest.sha1Hex(INPUT));
    }

    @Test
    void sha1OfText() {
        assertArrayEquals(Digest.sha1(utf8()), Digest.sha1(INPUT));
    }

    @Test
    void sha256OfText() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Hex.encode(Digest.sha256(INPUT)));
    }

    @Test
    void sha384OfText() {
        assertEquals("59e1748777448c69de6b800d7a33bbfb9ff1b463e44354c3553bcdb9c666fa90125a3c79f90397bdf5f6a13de828684f",
                Hex.encode(Digest.sha384(INPUT)));
    }

    @Test
    void sha512OfText() {
        assertEquals("9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da"
                + "0c4663475c2e5c3adef46f73bcdec043", Hex.encode(Digest.sha512(INPUT)));
    }

    @Test
    void digestsNonAsciiTextAsUtf8() {
        assertArrayEquals(Digest.sha256("ключ".getBytes(StandardCharsets.UTF_8)), Digest.sha256("ключ"));
    }

    @Test
    void jsonByteArrayLiteralIsTreatedAsText() {
        // The pre-fix behaviour digested the bytes 104, 105 ("hi"); it is now the literal text
        assertArrayEquals(Digest.sha256("[104,105]".getBytes(StandardCharsets.UTF_8)), Digest.sha256("[104,105]"));
    }
}
