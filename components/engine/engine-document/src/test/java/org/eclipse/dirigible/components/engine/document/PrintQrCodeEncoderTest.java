/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.eclipse.dirigible.parsers.document.renderer.QrCodeLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The image {@link PrintQrCodeEncoder} draws, scanned back: what a template author wrote is what a
 * phone reads.
 */
class PrintQrCodeEncoderTest {

    private static final String DATA_URI_PREFIX = "data:image/png;base64,";

    /** An EPC069-12 payment code: twelve lines, three of them empty, a Cyrillic beneficiary. */
    static final String EPC_PAYLOAD =
            "BCD\n002\n1\nSCT\nUNCRBGSF\nМетафор ООД\nBG80BNBG96611020345678\nEUR1250.50\n\n\nФактура SI-42 / Invoice SI-42\nБлагодарим!";

    private final PrintQrCodeEncoder encoder = new PrintQrCodeEncoder();

    @ParameterizedTest
    @EnumSource(QrCodeLevel.class)
    void aMultiLineCyrillicPayloadScansBackExactlyAtEveryLevel(QrCodeLevel level) throws Exception {
        assertEquals(EPC_PAYLOAD, QrCodeScanner.scan(image(encoder.encode(EPC_PAYLOAD, level))));
    }

    @Test
    void aTrailingEmptyLineIsPartOfThePayload() throws Exception {
        String payload = "BCD\n002\n\n";

        assertEquals(payload, QrCodeScanner.scan(image(encoder.encode(payload, QrCodeLevel.M))));
    }

    @Test
    void aSingleLinePayloadScansBack() throws Exception {
        String payload = "https://example.com/invoices/SI-42?paid=false&lang=bg";

        assertEquals(payload, QrCodeScanner.scan(image(encoder.encode(payload, QrCodeLevel.M))));
    }

    @Test
    void theImageIsSquareWithTheQuietZoneAroundTheSymbol() throws Exception {
        BufferedImage image = image(encoder.encode("x", QrCodeLevel.M));

        // version 1 is 21 modules; plus four modules of margin on each side, eight pixels a module
        assertEquals((21 + 8) * 8, image.getWidth());
        assertEquals(image.getWidth(), image.getHeight());
        assertEquals(0xFFFFFFFF, image.getRGB(31, 31), "the quiet zone is white");
        assertEquals(0xFF000000, image.getRGB(32, 32), "the finder pattern starts right after it");
    }

    @Test
    void aHigherLevelMakesADenserSymbolForTheSamePayload() throws Exception {
        int low = image(encoder.encode(EPC_PAYLOAD, QrCodeLevel.L)).getWidth();
        int high = image(encoder.encode(EPC_PAYLOAD, QrCodeLevel.H)).getWidth();

        assertTrue(high > low, "level H (" + high + "px) should need more modules than level L (" + low + "px)");
    }

    @Test
    void aPayloadTooLongForAQrCodeIsDeclinedSoftly() {
        // a version 40 symbol holds 1273 bytes at level H
        assertNull(encoder.encode("я".repeat(700), QrCodeLevel.H));
        assertNotNull(encoder.encode("я".repeat(600), QrCodeLevel.H));
    }

    static BufferedImage image(String dataUri) throws IOException {
        assertNotNull(dataUri, "the payload should have been encoded");
        assertTrue(dataUri.startsWith(DATA_URI_PREFIX), "the image is inlined as a PNG data URI");
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder()
                                                           .decode(dataUri.substring(DATA_URI_PREFIX.length()))));
    }
}
