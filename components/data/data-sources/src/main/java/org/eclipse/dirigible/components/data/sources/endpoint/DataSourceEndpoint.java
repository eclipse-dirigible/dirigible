/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.validation.Valid;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The Class DataSourceEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "sources")
public class DataSourceEndpoint extends BaseEndpoint {

    /** The data source service. */
    private final DataSourceService datasourceService;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceService the datasource service
     */
    @Autowired
    public DataSourceEndpoint(DataSourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<DataSource> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {
        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<DataSource> tables = datasourceService.getPages(pageable);
        return tables;
    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataSource> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(datasourceService.findById(id));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<DataSource> findByName(@RequestParam("name") String name) {
        return ResponseEntity.ok(datasourceService.findByName(name));
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<DataSource>> getAll() {
        return ResponseEntity.ok(datasourceService.getAll());
    }

    /**
     * Creates the data source.
     *
     * @param datasourceParameter the datasource parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PostMapping
    public ResponseEntity<URI> createDataSource(@Valid @RequestBody DataSourceParameter datasourceParameter) throws URISyntaxException {
        DataSource datasource =
                new DataSource("API_" + datasourceParameter.getName(), datasourceParameter.getName(), "", datasourceParameter.getDriver(),
                        datasourceParameter.getUrl(), datasourceParameter.getUsername(), datasourceParameter.getPassword());

        if (datasourceParameter.getParameters() != null && !datasourceParameter.getParameters()
                                                                               .isEmpty()) {
            StringTokenizer tokenizer = new StringTokenizer(datasourceParameter.getParameters(), ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken()
                                        .trim();
                if (!token.isEmpty()) {
                    int index = token.indexOf('=');
                    if (index > 0) {
                        String name = token.substring(0, index)
                                           .trim();
                        String value = token.substring(index + 1)
                                            .trim();
                        datasource.addProperty(name, value);
                    }
                }
            }
        }

        datasource.updateKey();
        datasource = datasourceService.save(datasource);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_DATA + "sources/" + datasource.getId()))
                             .build();
    }

    /**
     * Updates the data source.
     *
     * @param id the id of the data source
     * @param datasourceParameter the datasource parameter
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @PutMapping("{id}")
    public ResponseEntity<URI> updateDataSource(@PathVariable("id") Long id, @Valid @RequestBody DataSourceParameter datasourceParameter)
            throws URISyntaxException {
        DataSource datasource =
                new DataSource("API_" + datasourceParameter.getName(), datasourceParameter.getName(), "", datasourceParameter.getDriver(),
                        datasourceParameter.getUrl(), datasourceParameter.getUsername(), datasourceParameter.getPassword());
        datasource.setId(id);
        datasource.updateKey();
        datasource = datasourceService.save(datasource);
        return ResponseEntity.created(new URI(BaseEndpoint.PREFIX_ENDPOINT_DATA + "sources/" + datasource.getId()))
                             .build();
    }

    /**
     * Deletes the data source.
     *
     * @param id the id of the data source
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping("{id}")
    public ResponseEntity<URI> deleteDataSource(@PathVariable("id") Long id) throws URISyntaxException {
        DataSource datasource = datasourceService.findById(id);
        datasourceService.delete(datasource);
        return ResponseEntity.noContent()
                             .build();
    }

}
