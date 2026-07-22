/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.print;

import java.io.IOException;

import org.eclipse.dirigible.components.engine.document.PrintFacade;
import org.eclipse.dirigible.sdk.component.Beans;

/**
 * Client SDK for server-side print rendering: render a document's CMS print template to PDF bytes
 * from data the caller already holds. The counterpart to the browser's Print button (which POSTs
 * its own data) - for server-initiated renders such as the generated snapshot delegate that stores
 * an immutable printed copy of an invoice on issue.
 *
 * <p>
 * Pair with a {@code PrintFeeder} to obtain the {@code {document, items}} payload for a record,
 * then {@link org.eclipse.dirigible.sdk.cms.Attachments#store} the returned PDF as a snapshot.
 */
public final class Print {

    private Print() {}

    /**
     * Render the entity's print template (for the language) with the given {@code {document, items}}
     * JSON payload to PDF bytes.
     *
     * @param entity the document entity name (the {@code Templates/<entity>/Print/<language>} folder)
     * @param language the template language code (e.g. {@code en})
     * @param dataJson the {@code {document, items}} payload as JSON (e.g. a {@code PrintFeeder}'s
     *        output)
     * @return the rendered PDF
     */
    public static byte[] render(String entity, String language, String dataJson) {
        try {
            return Beans.get(PrintFacade.class)
                        .renderToPdf(entity, language, dataJson);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render the print for [" + entity + "] / [" + language + "]", e);
        }
    }
}
