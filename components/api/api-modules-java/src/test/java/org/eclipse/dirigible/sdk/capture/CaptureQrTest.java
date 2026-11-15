/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.sdk.utils.QrCode;
import org.junit.jupiter.api.Test;

class CaptureQrTest {

    @Test
    void decodesAGeneratedQr() throws Exception {
        byte[] png = QrCode.generate("hello capture");
        assertEquals(List.of("hello capture"), CaptureQr.decode("image/png", png));
    }

    @Test
    void readsTheSeparatedParts() throws Exception {
        byte[] png = QrCode.generate("123456*2026-11-15*A 1*12.50*2.50");
        Map<String, String> parts = CaptureQr.read("image/png", png, "*", "Fiscal", "Date", "Doc", "Total", "Vat");
        assertEquals("123456", parts.get("Fiscal"));
        assertEquals("2026-11-15", parts.get("Date"));
        assertEquals("A 1", parts.get("Doc"));
        assertEquals("12.50", parts.get("Total"));
        assertEquals("2.50", parts.get("Vat"));
        assertEquals(List.of("Fiscal", "Date", "Doc", "Total", "Vat"), List.copyOf(parts.keySet()));
    }

    @Test
    void aPartCountMismatchGivesNothing() throws Exception {
        byte[] png = QrCode.generate("a*b*c");
        assertTrue(CaptureQr.read("image/png", png, "*", "x", "y")
                            .isEmpty());
    }

    @Test
    void nonImagesAndGarbageGiveNothing() throws Exception {
        assertTrue(CaptureQr.decode("application/pdf", QrCode.generate("x"))
                            .isEmpty());
        assertTrue(CaptureQr.decode("image/png", new byte[] {1, 2, 3})
                            .isEmpty());
        assertTrue(CaptureQr.decode("image/png", null)
                            .isEmpty());
        assertTrue(CaptureQr.read("application/pdf", new byte[] {1}, "*", "a")
                            .isEmpty());
    }
}
