/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The QR code a {@link CaptureIntent} decodes before the model reads the document. Its text is
 * split on {@link #separator} into {@link #parts}, named in the order they are printed in - which
 * is the issuer's convention, so the intent states it rather than the platform assuming one:
 *
 * <pre>
 * qr: { separator: "*", parts: [device, receipt, date, time, total] }
 * </pre>
 *
 * A code that does not split into exactly that many parts is ignored. The parts are handed to the
 * model as known values, and a capture field naming one ({@code qr: total}) is checked against it.
 */
public class CaptureQrIntent {

    private String separator;
    private List<String> parts = new ArrayList<>();

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public List<String> getParts() {
        return parts;
    }

    public void setParts(List<String> parts) {
        this.parts = parts == null ? new ArrayList<>() : parts;
    }
}
