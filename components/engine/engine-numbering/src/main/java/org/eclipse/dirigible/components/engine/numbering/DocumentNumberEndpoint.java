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
 * Management surface for the current tenant's document-number series, backing the application
 * shell's "Document Numbering" settings page. Lists every series the tenant has - each declared by
 * a {@code .numbers} artefact and provisioned per tenant by its synchronizer - and lets an
 * administrator set its shape (prefix + total width) and the next value it will allocate.
 *
 * <p>
 * Configuring the shape here is what lets one application serve jurisdictions with different
 * numbering conventions without forking or regenerating it, and setting prefix + next together is
 * how a market that restarts numbering every January does so.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "numbering")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class DocumentNumberEndpoint extends BaseEndpoint {

    private final DocumentNumberService service;

    DocumentNumberEndpoint(DocumentNumberService service) {
        this.service = service;
    }

    /** The current tenant's series. */
    @GetMapping
    public ResponseEntity<List<SeriesView>> list() {
        try {
            return ResponseEntity.ok(service.list()
                                            .stream()
                                            .map(row -> new SeriesView(row.series(), row.partition(), row.prefix(), row.size(),
                                                    row.counter(), row.counter() + 1,
                                                    DocumentNumberService.render(row.prefix(), row.size(), row.counter() + 1)))
                                            .toList());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list document-number series", ex);
        }
    }

    /** Set the next value a series will allocate. */
    @PutMapping
    public ResponseEntity<Void> setNext(@RequestBody SetNextRequest request) {
        requireSeries(request == null ? null : request.series());
        try {
            service.setNext(request.series(), request.partition(), request.next());
            return ResponseEntity.noContent()
                                 .build();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set the document-number counter", ex);
        }
    }

    /** Set a series' shape: the literal prefix and the total rendered width. */
    @PutMapping("/shape")
    public ResponseEntity<Void> setShape(@RequestBody SetShapeRequest request) {
        requireSeries(request == null ? null : request.series());
        try {
            service.setShape(request.series(), request.partition(), request.prefix(), request.size());
            return ResponseEntity.noContent()
                                 .build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set the document-number shape", ex);
        }
    }

    private static void requireSeries(String series) {
        if (series == null || series.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "series is required");
        }
    }

    /**
     * Request body for setting a series' next value.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""}/{@code null} when unpartitioned)
     * @param next the next value the series should allocate
     */
    record SetNextRequest(String series, String partition, long next) {
    }

    /**
     * Request body for setting a series' shape.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""}/{@code null} when unpartitioned)
     * @param prefix the literal prefix - an EMPTY string is meaningful (no prefix at all)
     * @param size the total rendered width
     */
    record SetShapeRequest(String series, String partition, String prefix, int size) {
    }

    /**
     * One series as the settings page sees it.
     *
     * @param series the series identity
     * @param partition the partition value
     * @param prefix the literal prefix
     * @param size the total rendered width
     * @param counter the last allocated value
     * @param next the value the next document will get
     * @param example the next number as it will actually render - so an administrator sees the effect
     *        of a prefix or width change without issuing a document
     */
    record SeriesView(String series, String partition, String prefix, int size, long counter, long next, String example) {
    }
}
