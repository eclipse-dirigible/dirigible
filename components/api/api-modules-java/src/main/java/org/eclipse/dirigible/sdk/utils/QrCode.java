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

import com.google.zxing.WriterException;
import java.io.IOException;
import org.eclipse.dirigible.components.api.utils.QRCodeFacade;

/**
 * Renders a QR-code PNG for the given payload. Backed by ZXing under the hood with the platform's
 * default size and error-correction settings; ideal for embedding small URLs, codes, or connection
 * strings in receipts, emails, and IDE perspectives.
 * <p>
 * Returns the raw PNG bytes — write them straight to an HTTP response (with
 * {@code Content-Type: image/png}) or into a file via {@code Files.writeBytesNative}.
 */
public final class QrCode {

    private QrCode() {}

    public static byte[] generate(String text) throws WriterException, IOException {
        return QRCodeFacade.generateQRCode(text);
    }
}
