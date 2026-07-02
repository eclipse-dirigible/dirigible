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
 * Transforms an XML data document through an XSLT/XSL-FO stylesheet with Apache FOP and returns the
 * resulting PDF bytes — useful for invoice, report, and certificate generation without spinning up
 * a full reporting engine. Placeholder substitution is NOT performed here: expand the stylesheet
 * with a template engine (Mustache/Velocity) first, or use the document-template DSL pipeline
 * ({@code dirigible-parsers-document}), which produces a ready XSL-FO stylesheet.
 * <p>
 * Write the returned bytes straight into an HTTP response with
 * {@code Content-Type: application/pdf}, or into the repository / filesystem via
 * {@link org.eclipse.dirigible.sdk.io.Files#writeBytesNative(String, byte[])
 * Files.writeBytesNative}.
 */
public final class Pdf {

    private Pdf() {}

    /**
     * Generates a PDF.
     *
     * @param template the XSLT/XSL-FO stylesheet ({@code xmlns:fo="http://www.w3.org/1999/XSL/Format"})
     * @param dataXml the XML data document the stylesheet transforms
     * @return the PDF bytes
     */
    public static byte[] generate(String template, String dataXml) {
        return PDFFacade.generate(template, dataXml);
    }
}
