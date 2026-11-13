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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.eclipse.dirigible.parsers.document.renderer.QrCodeEncoder;
import org.eclipse.dirigible.parsers.document.renderer.QrCodeLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;

/**
 * Encodes the payload of a {@code .print} template's {@code <qrcode>} into a PNG, inlined as a
 * {@code data:} URI for the same reason {@link PrintImageResolver} inlines an image: the rendered
 * stylesheet is self-contained.
 *
 * <p>
 * The payload is written as its <b>UTF-8 bytes, without an ECI header</b>. The payment code this
 * element exists for - EPC069-12, "SEPA credit transfer" - declares its character set in its own
 * third line and asks for no ECI, and for any other text UTF-8 is what scanners detect by
 * themselves, so a Cyrillic beneficiary name survives. An ASCII-only payload still packs into the
 * denser numeric or alphanumeric mode when it fits one.
 *
 * <p>
 * The image is one black-or-white pixel block per module with the four-module quiet zone the
 * standard requires around the symbol, so the {@code size} of the element is the printed side
 * including that margin. A payload longer than a QR code can hold at the requested level is a
 * <b>soft</b> failure - {@code null}, which the renderer prints as nothing: a remittance text that
 * grew too long must not cost the invoice.
 */
final class PrintQrCodeEncoder implements QrCodeEncoder {

    private static final Logger logger = LoggerFactory.getLogger(PrintQrCodeEncoder.class);

    /** The blank margin around the symbol, in modules (ISO/IEC 18004). */
    private static final int QUIET_ZONE = 4;

    /** The pixels one module is drawn with - enough for the symbol to stay crisp when it is scaled. */
    private static final int MODULE_PIXELS = 8;

    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;

    @Override
    public String encode(String payload, QrCodeLevel level) {
        try {
            ByteMatrix modules = Encoder.encode(utf8BytesAsChars(payload), errorCorrection(level))
                                        .getMatrix();
            return "data:image/png;base64," + Base64.getEncoder()
                                                    .encodeToString(png(modules));
        } catch (WriterException e) {
            logger.warn("The payload of a print QR code ({} characters) does not fit a QR code at level {} - printing without it",
                    payload.length(), level, e);
            return null;
        } catch (IOException e) {
            logger.warn("Failed to write the image of a print QR code - printing without it", e);
            return null;
        }
    }

    /**
     * The payload's UTF-8 bytes, one {@code char} per byte: the encoder writes a byte-mode segment in
     * ISO-8859-1 when it is given no character set, which maps every such {@code char} back to its byte
     * - the UTF-8 form on paper, and no ECI header announcing it.
     */
    private static String utf8BytesAsChars(String payload) {
        return new String(payload.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
    }

    private static ErrorCorrectionLevel errorCorrection(QrCodeLevel level) {
        return switch (level) {
            case L -> ErrorCorrectionLevel.L;
            case M -> ErrorCorrectionLevel.M;
            case Q -> ErrorCorrectionLevel.Q;
            case H -> ErrorCorrectionLevel.H;
        };
    }

    private static byte[] png(ByteMatrix modules) throws IOException {
        int side = (modules.getWidth() + 2 * QUIET_ZONE) * MODULE_PIXELS;
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                image.setRGB(x, y, isDark(modules, x / MODULE_PIXELS - QUIET_ZONE, y / MODULE_PIXELS - QUIET_ZONE) ? BLACK : WHITE);
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return png.toByteArray();
    }

    private static boolean isDark(ByteMatrix modules, int column, int row) {
        return column >= 0 && row >= 0 && column < modules.getWidth() && row < modules.getHeight() && modules.get(column, row) == 1;
    }
}
