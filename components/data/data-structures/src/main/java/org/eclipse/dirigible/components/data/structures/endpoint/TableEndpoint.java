/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The Class TableEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "tables")
public class TableEndpoint extends BaseEndpoint {

    /** The table service. */
    private final TableService tableService;

    /**
     * Instantiates a new table endpoint.
     *
     * @param tableService the table service
     */
    @Autowired
    public TableEndpoint(TableService tableService) {
        this.tableService = tableService;
    }

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<Table> findAll(@Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {
        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Table> tables = tableService.getPages(pageable);
        return tables;
    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Table> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(tableService.findById(id));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<Table> findByName(@RequestParam("name") String name) {
        return ResponseEntity.ok(tableService.findByName(name));
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<Table>> getAll() {
        return ResponseEntity.ok(tableService.getAll());
    }

}
