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

import java.awt.image.BufferedImage;
import java.util.Map;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ReaderException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

/**
 * Reads the QR code off an image the way a phone does - no character set is suggested, so the text
 * that comes back is what a scanner detects by itself.
 */
final class QrCodeScanner {

    private QrCodeScanner() {}

    /**
     * Scans the one QR code in the image.
     *
     * @param image the image - the symbol alone, or a page carrying it
     * @return the decoded text
     * @throws ReaderException when the image carries no readable QR code
     */
    static String scan(BufferedImage image) throws ReaderException {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
        return new QRCodeMultiReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE))
                                      .getText();
    }
}
