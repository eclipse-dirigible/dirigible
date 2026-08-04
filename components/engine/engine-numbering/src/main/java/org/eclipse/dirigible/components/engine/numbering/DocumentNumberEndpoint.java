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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final NumberSeriesDeclarationService declarationService;

    DocumentNumberEndpoint(DocumentNumberService service, NumberSeriesDeclarationService declarationService) {
        this.service = service;
        this.declarationService = declarationService;
    }

    /** The current tenant's series. */
    @GetMapping
    public ResponseEntity<List<SeriesView>> list() {
        try {
            List<DocumentNumberStore.Series> rows = service.list();
            // The declared PARTITION SOURCES: which physical table a series' partition values come
            // from, keyed by series. What lets a partition row be labeled by the entity's display
            // name and lets every value appear BEFORE its first allocation (a virtual row), so an
            // operator can seed a company's starting number before its first document.
            Map<String, NumberSeriesDeclaration> partitionSources = new LinkedHashMap<>();
            for (NumberSeriesDeclaration declaration : declarationService.getAll()) {
                if (declaration.getPartitionTable() != null && !declaration.getPartitionTable()
                                                                           .isBlank()) {
                    partitionSources.putIfAbsent(declaration.getName(), declaration);
                }
            }
            Map<String, Map<String, String>> labelsBySeries = new LinkedHashMap<>();
            for (Map.Entry<String, NumberSeriesDeclaration> source : partitionSources.entrySet()) {
                Map<String, String> labels = new LinkedHashMap<>();
                for (DocumentNumberStore.PartitionValue value : service.partitionSource(source.getValue()
                                                                                              .getPartitionTable(),
                        source.getValue()
                              .getPartitionKey(),
                        source.getValue()
                              .getPartitionLabel())) {
                    labels.put(value.value(), value.label());
                }
                labelsBySeries.put(source.getKey(), labels);
            }

            // A series is PARTITIONED once any of its partition rows exists OR its declaration names
            // a partition source. The base ("") row of a partitioned series is only the shape template
            // partitions inherit at birth - allocation never draws from it - so the settings page must
            // not offer its counter for editing.
            Set<String> partitionedSeries = rows.stream()
                                                .filter(row -> !row.partition()
                                                                   .isEmpty())
                                                .map(DocumentNumberStore.Series::series)
                                                .collect(Collectors.toCollection(HashSet::new));
            partitionedSeries.addAll(labelsBySeries.keySet());

            List<SeriesView> views = new ArrayList<>();
            Map<String, DocumentNumberStore.Series> baseRows = new LinkedHashMap<>();
            Set<String> materialized = new HashSet<>();
            for (DocumentNumberStore.Series row : rows) {
                if (row.partition()
                       .isEmpty()) {
                    baseRows.put(row.series(), row);
                }
                materialized.add(row.series() + "|" + row.partition());
                Map<String, String> labels = labelsBySeries.getOrDefault(row.series(), Map.of());
                views.add(new SeriesView(row.series(), row.partition(), labels.get(row.partition()), row.prefix(), row.size(),
                        row.counter(), row.counter() + 1, DocumentNumberService.render(row.prefix(), row.size(), row.counter() + 1),
                        partitionedSeries.contains(row.series()), false));
            }
            // VIRTUAL rows: every declared partition value with no materialized row yet, rendered
            // with the base row's shape and a fresh sequence - saving one provisions it.
            for (Map.Entry<String, Map<String, String>> entry : labelsBySeries.entrySet()) {
                DocumentNumberStore.Series base = baseRows.get(entry.getKey());
                if (base == null) {
                    continue; // the series is declared but not provisioned for this tenant yet
                }
                for (Map.Entry<String, String> value : entry.getValue()
                                                            .entrySet()) {
                    if (materialized.contains(entry.getKey() + "|" + value.getKey())) {
                        continue;
                    }
                    views.add(new SeriesView(entry.getKey(), value.getKey(), value.getValue(), base.prefix(), base.size(), 0, 1,
                            DocumentNumberService.render(base.prefix(), base.size(), 1), true, true));
                }
            }
            return ResponseEntity.ok(views);
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
     * @param partitionLabel the partition's display label from the declared partition source, or
     *        {@code null} when the series declares none / the value is not in the source
     * @param partitioned whether the series has partition rows or declares a partition source - the
     *        base row of a partitioned series is only the shape template new partitions inherit, so its
     *        counter is not editable
     * @param virtual whether the row is not materialized yet - a declared partition value that has
     *        never allocated; saving it provisions the row (an operator seeds a partition's starting
     *        number BEFORE its first document)
     */
    record SeriesView(String series, String partition, String partitionLabel, String prefix, int size, long counter, long next,
            String example, boolean partitioned, boolean virtual) {
    }
}
