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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The print surface of generated applications:
 * <ul>
 * <li>{@code GET /services/print/{entity}/languages} — the languages a print template exists for
 * (the child folders of the entity's CMS {@code Print} folder).</li>
 * <li>{@code POST /services/print/{entity}?lang=en} — renders the entity's CMS print template with
 * the posted JSON data and responds with the PDF.</li>
 * </ul>
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "print")
class PrintEndpoint extends BaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PrintEndpoint.class);

    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * A plain Gson (no {@code @Expose} filtering) keeping JSON integers as longs — the data is
     * map-shaped, not POJO-shaped.
     */
    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                                                      .create();

    private final CmsStore cmsStore;

    PrintEndpoint(CmsStore cmsStore) {
        this.cmsStore = cmsStore;
    }

    /**
     * Lists the languages the given entity has print templates for.
     *
     * @param entity the domain entity name
     * @return a JSON array of {@code {"code": "en", "name": "English"}} entries, empty when the entity
     *         has no templates
     */
    @GetMapping(value = "/{entity}/languages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Map<String, String>>> getLanguages(@PathVariable("entity") String entity) {
        try {
            List<Map<String, String>> languages = cmsStore.listLanguages(entity)
                                                          .stream()
                                                          .map(code -> Map.of("code", code, "name", displayName(code)))
                                                          .toList();
            return ResponseEntity.ok(languages);
        } catch (IOException e) {
            logger.error("Failed to list print template languages for entity [{}]", entity, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to list print template languages for entity [" + entity + "]", e);
        }
    }

    /**
     * Renders the entity's print template with the posted data and responds with the PDF.
     *
     * @param entity the domain entity name
     * @param language the template language, defaults to {@code en}
     * @param body the JSON data context, e.g. {@code {"document": {...}, "items": [...]}}
     * @return the PDF bytes, served inline as {@code <entity>.pdf}
     */
    @PostMapping(value = "/{entity}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> print(@PathVariable("entity") String entity,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language, @RequestBody String body) {
        String templateSource = findTemplate(entity, language);
        Map<String, Object> data = GSON.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType());

        byte[] pdf = PrintRenderer.renderPdf(templateSource, data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                                                        .filename(entity + ".pdf")
                                                        .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private String findTemplate(String entity, String language) {
        try {
            return cmsStore.findTemplate(entity, language)
                           .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                   "No print template found for entity [" + entity + "] and language [" + language + "]"));
        } catch (IOException e) {
            logger.error("Failed to read the print template for entity [{}] and language [{}]", entity, language, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read the print template for entity [" + entity + "] and language [" + language + "]", e);
        }
    }

    private static String displayName(String code) {
        String name = Locale.forLanguageTag(code)
                            .getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() ? code : name;
    }
}
