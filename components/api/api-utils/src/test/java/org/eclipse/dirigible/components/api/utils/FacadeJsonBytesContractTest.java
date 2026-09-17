/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The {@code (String)} overloads of these facades read their argument as a JSON array of byte
 * values — the form a GraalJS byte array arrives in. The Java SDK wrappers in
 * {@code org.eclipse.dirigible.sdk.utils} deliberately no longer route through them (issue #7042),
 * so this pins the convention the JS/TS surface still depends on.
 */
class FacadeJsonBytesContractTest {

    /** {@code [104,105]} is "hi". */
    private static final String HI_AS_JSON_BYTES = "[104,105]";

    @Test
    void base64EncodeReadsJsonByteArray() {
        assertEquals("aGk=", Base64Facade.encode(HI_AS_JSON_BYTES));
    }

    @Test
    void hexEncodeReadsJsonByteArray() {
        assertEquals("6869", HexFacade.encode(HI_AS_JSON_BYTES));
    }

    @Test
    void digestReadsJsonByteArray() {
        assertEquals("49f68a5c8493ec2c0bf489821c21fc3b", DigestFacade.md5Hex(HI_AS_JSON_BYTES));
        assertArrayEquals(DigestFacade.sha256(new byte[] {104, 105}), DigestFacade.sha256(HI_AS_JSON_BYTES));
    }
}
