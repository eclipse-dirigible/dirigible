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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

/**
 * The {@code encode(String)} counterpart of {@link Base64Test}: hex-encoding text encodes its UTF-8
 * bytes, so it round-trips through {@link Hex#decode(String)}.
 */
class HexTest {

    @Test
    void encodesTextAsUtf8Bytes() {
        assertEquals("48656c6c6f2c20576f726c6421", Hex.encode("Hello, World!"));
    }

    @Test
    void encodesEmptyText() {
        assertEquals("", Hex.encode(""));
    }

    @Test
    void stringOverloadAgreesWithBytesOverload() {
        String text = "ключ";
        assertEquals(Hex.encode(text.getBytes(StandardCharsets.UTF_8)), Hex.encode(text));
    }

    @Test
    void decodeRoundTripsEncode() throws DecoderException {
        String text = "Hello, World! - ключ";
        assertEquals(text, new String(Hex.decode(Hex.encode(text)), StandardCharsets.UTF_8));
    }
}
