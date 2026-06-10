/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.pdf;

import org.eclipse.dirigible.components.api.pdf.PDFFacade;

/**
 * Renders a Mustache-style template against a JSON data document and returns the resulting PDF
 * bytes. The platform handles font loading, page sizing, and basic CSS — useful for invoice,
 * report, and certificate generation without spinning up a full reporting engine.
 * <p>
 * Write the returned bytes straight into an HTTP response with
 * {@code Content-Type: application/pdf}, or into the repository / filesystem via
 * {@link org.eclipse.dirigible.sdk.io.Files#writeBytesNative(String, byte[])
 * Files.writeBytesNative}.
 */
public final class Pdf {

    private Pdf() {}

    public static byte[] generate(String template, String dataJson) {
        return PDFFacade.generate(template, dataJson);
    }
}
