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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decodes QR codes printed on a document image, so exact values need not be read by the model. */
public final class CaptureQr {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptureQr.class);
    private static final int DOWNSCALE_ABOVE = 2000;
    private static final int DOWNSCALE_TO = 1200;

    private CaptureQr() {}

    /**
     * Decode every QR code found in an image.
     *
     * @param contentType the media type; a non-image type yields an empty list
     * @param content the image bytes (anything ImageIO reads)
     * @return the decoded texts; empty for a non-image, an unreadable image or no QR code. Never throws
     */
    public static List<String> decode(String contentType, byte[] content) {
        List<String> texts = new ArrayList<>();
        try {
            if (content == null || content.length == 0 || (contentType != null && !contentType.trim()
                                                                                              .toLowerCase(Locale.ROOT)
                                                                                              .startsWith("image/"))) {
                return texts;
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                return texts;
            }
            texts = scan(image);
            if (texts.isEmpty() && Math.max(image.getWidth(), image.getHeight()) > DOWNSCALE_ABOVE) {
                texts = scan(downscale(image));
            }
        } catch (Throwable e) {
            LOGGER.debug("Cannot decode a QR code", e);
        }
        return texts;
    }

    /**
     * Split the first decoded QR text that has exactly as many parts as names.
     *
     * @param contentType the media type
     * @param content the image bytes
     * @param separator the literal separator between the parts
     * @param names the part names, in order
     * @return name to trimmed part; empty when no QR text matches
     */
    public static Map<String, String> read(String contentType, byte[] content, String separator, String... names) {
        Map<String, String> result = new LinkedHashMap<>();
        if (separator == null || separator.isEmpty() || names == null || names.length == 0) {
            return result;
        }
        for (String text : decode(contentType, content)) {
            String[] parts = text.split(Pattern.quote(separator), -1);
            if (parts.length == names.length) {
                for (int i = 0; i < names.length; i++) {
                    result.put(names[i], parts[i].trim());
                }
                return result;
            }
        }
        return result;
    }

    private static List<String> scan(BufferedImage image) {
        List<String> texts = new ArrayList<>();
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        try {
            Result[] results = new QRCodeMultiReader().decodeMultiple(bitmap,
                    Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE, DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)));
            for (Result result : results) {
                texts.add(result.getText());
            }
        } catch (NotFoundException e) {
            // no QR code in this image
        }
        return texts;
    }

    private static BufferedImage downscale(BufferedImage image) {
        double ratio = (double) DOWNSCALE_TO / Math.max(image.getWidth(), image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * ratio));
        int h = Math.max(1, (int) Math.round(image.getHeight() * ratio));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }
}
