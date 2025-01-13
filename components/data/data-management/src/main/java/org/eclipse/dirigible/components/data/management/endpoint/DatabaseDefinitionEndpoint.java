/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.endpoint;

import java.sql.SQLException;
import java.util.Set;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.management.service.DatabaseDefinitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * The Class DataSourceMetadataEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "definition")
public class DatabaseDefinitionEndpoint extends BaseEndpoint {

    /** The databases service. */
    private final DatabaseDefinitionService databaseDefinitionService;

    /**
     * Instantiates a new data source metadata endpoint.
     *
     * @param databaseDefinitionService the database definition service
     */
    @Autowired
    public DatabaseDefinitionEndpoint(DatabaseDefinitionService databaseDefinitionService) {
        this.databaseDefinitionService = databaseDefinitionService;
    }

    /**
     * Gets the data sources.
     *
     * @return the data sources
     */
    @GetMapping(value = "/", produces = "application/json")
    public ResponseEntity<Set<String>> getDataSourcesNames() {
        return ResponseEntity.ok(databaseDefinitionService.getDataSourcesNames());
    }

    /**
     * Gets the data sources.
     *
     * @param datasource the datasource
     * @return the data sources
     * @throws SQLException the SQL exception
     */
    @GetMapping(value = "/{datasource}", produces = "application/json")
    public ResponseEntity<Set<String>> getSchemasNames(@PathVariable("datasource") String datasource) throws SQLException {
        return ResponseEntity.ok(databaseDefinitionService.getSchemasNames(datasource));
    }

    /**
     * Gets the metadata of a schema.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @return the response entity
     * @throws SQLException the SQL exception
     */
    @GetMapping(value = "/{datasource}/{schema}", produces = "application/json")
    public ResponseEntity<String> loadSchemaMetadata(@PathVariable("datasource") String datasource, @PathVariable("schema") String schema)
            throws SQLException {
        return ResponseEntity.ok(databaseDefinitionService.loadSchemaMetadata(datasource, schema));
    }

    /**
     * Gets the metadata of a structure.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param structure the structure
     * @return the response entity
     * @throws SQLException the SQL exception
     */
    @GetMapping(value = "/{datasource}/{schema}/{structure}", produces = "application/json")
    public ResponseEntity<String> loadStructureMetadata(@PathVariable("datasource") String datasource,
            @PathVariable("schema") String schema, @PathVariable("structure") String structure) throws SQLException {
        return ResponseEntity.ok(databaseDefinitionService.loadStructureMetadata(datasource, schema, structure));
    }

}
