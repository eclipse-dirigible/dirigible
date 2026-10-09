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
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The print surface of generated applications:
 * <ul>
 * <li>{@code GET /services/print/{entity}/languages} — the languages a print template exists for
 * (the child folders of the entity's CMS {@code Print} folder).</li>
 * <li>{@code POST /services/print/{entity}?lang=en&template=} — renders the entity's print template
 * (the requested one, else the active one) with the posted JSON data and responds with the
 * PDF.</li>
 * <li>{@code /services/print/{entity}/templates...} — the entity's template catalogue: list, read,
 * write, duplicate and delete (see {@link PrintTemplateCatalog}). Reading a source and changing the
 * catalogue is an administrator's task, and the CMS access grants of the template's path apply on
 * top, as they do in the Documents perspective; which template is active is a tenant configuration,
 * written through {@code PUT /services/core/configurations/tenant}, not here.</li>
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

    private final PrintTemplateCatalog catalog;
    private final PrintImageResolver imageResolver;

    PrintEndpoint(PrintTemplateCatalog catalog, PrintImageResolver imageResolver) {
        this.catalog = catalog;
        this.imageResolver = imageResolver;
    }

    /**
     * Lists the document types that may have print templates - their languages are listed by
     * {@code /{entity}/languages}.
     *
     * @return a JSON array of {@code {"entity": "SalesInvoice"}} entries
     */
    @GetMapping(value = "/document-types", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PrintTemplateCatalog.DocumentType>> getDocumentTypes() {
        try {
            return ResponseEntity.ok(catalog.documentTypes());
        } catch (IOException e) {
            throw serverError("Failed to list the document types with print templates", e);
        }
    }

    /**
     * Lists the languages the given entity has print templates for.
     *
     * @param entity the domain entity name
     * @return a JSON array of {@code {"code": "en", "name": "English", "selectionKey":
     *         "DIRIGIBLE_PRINT_TEMPLATE_SALESINVOICE_EN"}} entries - the selection key is the tenant
     *         configuration that selects the language's active template - empty when the entity has no
     *         templates
     */
    @GetMapping(value = "/{entity}/languages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Map<String, String>>> getLanguages(@PathVariable("entity") String entity) {
        try {
            List<Map<String, String>> languages = catalog.languages(entity)
                                                         .stream()
                                                         .map(code -> Map.of("code", code, "name", displayName(code), "selectionKey",
                                                                 PrintTemplateSelection.key(entity, code)))
                                                         .toList();
            return ResponseEntity.ok(languages);
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to list print template languages for entity [" + entity + "]", e);
        }
    }

    /**
     * Renders the entity's print template with the posted data and responds with the PDF.
     *
     * @param entity the domain entity name
     * @param language the template language, defaults to {@code en}
     * @param template the template to print with, defaults to the active one
     * @param body the JSON data context, e.g. {@code {"document": {...}, "items": [...]}}
     * @return the PDF bytes, served inline as {@code <entity>.pdf}
     */
    @PostMapping(value = "/{entity}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> print(@PathVariable("entity") String entity,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language,
            @RequestParam(name = "template", required = false) String template, @RequestBody String body) {
        String templateSource = resolve(entity, language, template);
        Map<String, Object> data = GSON.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType());

        byte[] pdf = PrintRenderer.renderPdf(templateSource, language, data, imageResolver);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                                                        .filename(entity + ".pdf")
                                                        .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Lists the entity's template catalogue for a language.
     *
     * @param entity the domain entity name
     * @param language the template language
     * @param details whether to read each tenant template for the version it derives from - the
     *        Settings page asks for it, a print dialog does not need it
     * @return the shipped versions newest first, then the tenant templates
     */
    @GetMapping(value = "/{entity}/templates", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PrintTemplateCatalog.Entry>> listTemplates(@PathVariable("entity") String entity,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language,
            @RequestParam(name = "details", defaultValue = "false") boolean details) {
        try {
            return ResponseEntity.ok(catalog.list(entity, language, details));
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to list the print templates for entity [" + entity + "] and language [" + language + "]", e);
        }
    }

    /**
     * Reads a template's source.
     *
     * @param entity the domain entity name
     * @param name the template reference
     * @param language the template language
     * @param request the request, for the CMS access grants
     * @return the source as plain text
     */
    @GetMapping(value = "/{entity}/templates/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    @RolesAllowed({Roles.RoleNames.ADMINISTRATOR, Roles.RoleNames.OPERATOR})
    ResponseEntity<String> readTemplate(@PathVariable("entity") String entity, @PathVariable("name") String name,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(catalog.read(entity, language, name, request));
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to read the print template [" + name + "]", e);
        }
    }

    /**
     * Writes a tenant template's source, creating the template when it does not exist yet; a shipped
     * version is refused with 409.
     *
     * @param entity the domain entity name
     * @param name the tenant template name
     * @param language the template language
     * @param source the template source
     * @param request the request, for the CMS access grants
     * @return an empty 204 response
     */
    @PutMapping(value = "/{entity}/templates/{name}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @RolesAllowed({Roles.RoleNames.ADMINISTRATOR, Roles.RoleNames.OPERATOR})
    ResponseEntity<Void> writeTemplate(@PathVariable("entity") String entity, @PathVariable("name") String name,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language, @RequestBody(required = false) String source,
            HttpServletRequest request) {
        try {
            catalog.write(entity, language, name, source == null ? "" : source, request);
            return ResponseEntity.noContent()
                                 .build();
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to write the print template [" + name + "]", e);
        }
    }

    /**
     * Copies a shipped version or a tenant template into a new tenant template.
     *
     * @param entity the domain entity name
     * @param name the template to copy
     * @param language the template language
     * @param target the new tenant template's name
     * @param request the request, for the CMS access grants
     * @return 201 with {@code {"name": "<target>"}}
     */
    @PostMapping(value = "/{entity}/templates/{name}/duplicate", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({Roles.RoleNames.ADMINISTRATOR, Roles.RoleNames.OPERATOR})
    ResponseEntity<Map<String, String>> duplicateTemplate(@PathVariable("entity") String entity, @PathVariable("name") String name,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language, @RequestParam("as") String target,
            HttpServletRequest request) {
        try {
            PrintTemplateName copy = catalog.duplicate(entity, language, name, target, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(Map.of("name", copy.reference()));
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to duplicate the print template [" + name + "]", e);
        }
    }

    /**
     * Deletes a tenant template; a shipped version and the active template are refused with 409.
     *
     * @param entity the domain entity name
     * @param name the tenant template name
     * @param language the template language
     * @param request the request, for the CMS access grants
     * @return an empty 204 response
     */
    @DeleteMapping(value = "/{entity}/templates/{name}")
    @RolesAllowed({Roles.RoleNames.ADMINISTRATOR, Roles.RoleNames.OPERATOR})
    ResponseEntity<Void> deleteTemplate(@PathVariable("entity") String entity, @PathVariable("name") String name,
            @RequestParam(name = "lang", defaultValue = DEFAULT_LANGUAGE) String language, HttpServletRequest request) {
        try {
            catalog.delete(entity, language, name, request);
            return ResponseEntity.noContent()
                                 .build();
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException | SQLException e) {
            throw serverError("Failed to delete the print template [" + name + "]", e);
        }
    }

    private String resolve(String entity, String language, String template) {
        try {
            return catalog.resolve(entity, language, template)
                          .source();
        } catch (PrintTemplateException e) {
            throw refused(e);
        } catch (IOException e) {
            throw serverError("Failed to read the print template for entity [" + entity + "] and language [" + language + "]", e);
        }
    }

    private static ResponseStatusException refused(PrintTemplateException e) {
        HttpStatus status = switch (e.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
        return new ResponseStatusException(status, e.getMessage(), e);
    }

    private static ResponseStatusException serverError(String message, Exception e) {
        logger.error("{}", LoggedPath.of(message), e);
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
    }

    private static String displayName(String code) {
        String name = Locale.forLanguageTag(code)
                            .getDisplayLanguage(Locale.ENGLISH);
        return name.isBlank() ? code : name;
    }
}
