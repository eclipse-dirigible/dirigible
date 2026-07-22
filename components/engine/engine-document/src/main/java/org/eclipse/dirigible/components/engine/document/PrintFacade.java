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

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

/**
 * Server-side print rendering for callers that already hold the data — chiefly the generated
 * snapshot delegate, which renders a document to an immutable PDF copy on issue. It is the
 * server-initiated counterpart to {@code PrintEndpoint}: where the endpoint takes the data the
 * browser POSTs, this resolves the entity's CMS print template for the language and renders the
 * supplied data map to PDF.
 *
 * <p>
 * The {@code String}-payload overload parses the same {@code {document, items}} shape a
 * {@code PrintFeeder} emits, with the SAME plain Gson number policy as the endpoint
 * ({@link ToNumberPolicy#LONG_OR_DOUBLE} — integers stay integral, decimals arrive as
 * {@code Double} for the money pattern; never the {@code @Expose} {@code JsonHelper}).
 */
@Component
public class PrintFacade {

    /** Plain Gson (no {@code @Expose} filtering), integers as longs — matches {@code PrintEndpoint}. */
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                                                      .create();

    private final CmsStore cmsStore;

    PrintFacade(CmsStore cmsStore) {
        this.cmsStore = cmsStore;
    }

    /**
     * Render the entity's print template for the language with the given data to PDF bytes.
     *
     * @param entity the document entity name (the {@code Templates/<entity>/Print/<language>} folder)
     * @param language the template language code (e.g. {@code en})
     * @param data the {@code {document, items}} data the template binds
     * @return the rendered PDF
     * @throws IOException if no template exists for the entity/language or the CMS read fails
     */
    public byte[] renderToPdf(String entity, String language, Map<String, Object> data) throws IOException {
        String template = cmsStore.findTemplate(entity, language)
                                  .orElseThrow(() -> new IOException(
                                          "No print template for entity [" + entity + "] and language [" + language + "]"));
        return PrintRenderer.renderPdf(template, data);
    }

    /**
     * Render from the {@code {document, items}} JSON payload (e.g. a {@code PrintFeeder}'s output).
     *
     * @param entity the document entity name
     * @param language the template language code
     * @param dataJson the {@code {document, items}} payload as JSON
     * @return the rendered PDF
     * @throws IOException if no template exists for the entity/language or the CMS read fails
     */
    public byte[] renderToPdf(String entity, String language, String dataJson) throws IOException {
        Map<String, Object> data = GSON.fromJson(dataJson, new TypeToken<Map<String, Object>>() {}.getType());
        return renderToPdf(entity, language, data);
    }
}
