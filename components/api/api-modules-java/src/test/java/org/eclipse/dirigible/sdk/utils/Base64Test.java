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
 * A {@link String} handed to the SDK is text, not the JSON byte array the shared facades accept for
 * the GraalJS surface. These assertions pin that: {@code encode} takes the argument's UTF-8 bytes,
 * which makes it agree with {@code decode} and with the {@code byte[]} overload.
 */
class Base64Test {

    @Test
    void encodesTextAsUtf8Bytes() {
        assertEquals("dXNlcjpzZWNyZXQ=", Base64.encode("user:secret"));
    }

    @Test
    void encodesEmptyText() {
        assertEquals("", Base64.encode(""));
    }

    @Test
    void encodesNonAsciiTextAsUtf8() {
        // "ключ" is 8 bytes in UTF-8, so the result must not depend on the platform charset
        assertEquals(Base64.encode("ключ".getBytes(StandardCharsets.UTF_8)), Base64.encode("ключ"));
    }

    @Test
    void stringOverloadAgreesWithBytesOverload() {
        String text = "Hello, World!";
        assertEquals(Base64.encode(text.getBytes(StandardCharsets.UTF_8)), Base64.encode(text));
    }

    @Test
    void decodeRoundTripsEncode() {
        String text = "Hello, World! - ключ";
        assertEquals(text, new String(Base64.decode(Base64.encode(text)), StandardCharsets.UTF_8));
    }

    @Test
    void jsonByteArrayLiteralIsTreatedAsText() {
        // The pre-fix behaviour parsed this as the bytes 104, 105 ("hi"); it is now plain text
        assertArrayEquals("[104,105]".getBytes(StandardCharsets.UTF_8), Base64.decode(Base64.encode("[104,105]")));
    }
}
