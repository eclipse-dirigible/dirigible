/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.RolesAllowed;

/**
 * Management surface for the current tenant's document-number counters, backing the application
 * shell's "Document Numbering" settings page. Lists the per-(series, scope) counters and lets an
 * administrator set the next value a counter will allocate (reset / seed a sequence).
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "numbering")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class DocumentNumberEndpoint extends BaseEndpoint {

    private final DocumentNumberService service;

    DocumentNumberEndpoint(DocumentNumberService service) {
        this.service = service;
    }

    /**
     * The current tenant's series: every series the running application DECLARED plus any counter that
     * predates declaration, with the tenant's format override and whether one is expressible at all.
     */
    @GetMapping
    public ResponseEntity<List<SeriesView>> list() {
        try {
            return ResponseEntity.ok(service.listAll()
                                            .stream()
                                            .map(counter -> new SeriesView(counter.series(), counter.scope(), counter.counter(),
                                                    counter.counter() + 1, counter.format(), counter.prefix(), counter.size(),
                                                    DocumentNumberService.overridable(counter.format())))
                                            .toList());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list document-number counters", ex);
        }
    }

    /**
     * Set (or clear) the tenant's prefix/size override for a series. Clearing both falls back to the
     * format the application authored.
     */
    @PutMapping("/format")
    public ResponseEntity<Void> setFormat(@RequestBody SetFormatRequest request) {
        if (request == null || request.series() == null || request.series()
                                                                  .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "series is required");
        }
        try {
            service.setOverride(request.series(), request.scope() == null ? "" : request.scope(), request.prefix(), request.size());
            return ResponseEntity.noContent()
                                 .build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set the document-number format", ex);
        }
    }

    /** Set the next value a (series, scope) counter will allocate. */
    @PutMapping
    public ResponseEntity<Void> setNext(@RequestBody SetNextRequest request) {
        if (request == null || request.series() == null || request.series()
                                                                  .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "series is required");
        }
        try {
            service.setNext(request.series(), request.scope() == null ? "" : request.scope(), request.next());
            return ResponseEntity.noContent()
                                 .build();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set the document-number counter", ex);
        }
    }

    /**
     * Request body for setting a counter's next value.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""}/{@code null} for an unscoped series)
     * @param next the next value the counter should allocate
     */
    record SetNextRequest(String series, String scope, long next) {
    }

    /**
     * Request body for the prefix/size override.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""}/{@code null} for an unscoped series)
     * @param prefix the literal prefix - an EMPTY string is meaningful (no prefix at all), null with a
     *        null size clears the override
     * @param size the total rendered width
     */
    record SetFormatRequest(String series, String scope, String prefix, Integer size) {
    }

    /**
     * One series as the settings page sees it.
     *
     * @param series the series identity
     * @param scope the scope key
     * @param counter the last allocated value
     * @param next the value the next document will get
     * @param format the format the application authored
     * @param prefix the tenant's prefix override, or null
     * @param size the tenant's width override, or null
     * @param overridable whether a prefix/size override can express this format at all (false when it
     *        carries scope tokens the override cannot reproduce)
     */
    record SeriesView(String series, String scope, long counter, long next, String format, String prefix, Integer size,
            boolean overridable) {
    }
}
