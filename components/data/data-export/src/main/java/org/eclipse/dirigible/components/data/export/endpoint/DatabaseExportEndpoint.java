/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.endpoint;

import static java.text.MessageFormat.format;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.export.service.DatabaseExportService;
import org.eclipse.dirigible.components.data.management.service.DatabaseMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Front facing REST service serving the raw data.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "export")
public class DatabaseExportEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExportEndpoint.class);

    /** The database export service. */
    private DatabaseExportService databaseExportService;

    /** The database metadata service. */
    private DatabaseMetadataService databaseMetadataService;

    /**
     * Instantiates a new database export endpoint.
     *
     * @param databaseExportService the database export service
     * @param databaseMetadataService the database metadata service
     */
    @Autowired
    public DatabaseExportEndpoint(DatabaseExportService databaseExportService, DatabaseMetadataService databaseMetadataService) {
        this.databaseExportService = databaseExportService;
        this.databaseMetadataService = databaseMetadataService;
    }

    /**
     * Gets the database export service.
     *
     * @return the database export service
     */
    public DatabaseExportService getDatabaseExportService() {
        return databaseExportService;
    }

    /**
     * Gets the database metadata service.
     *
     * @return the database metadata service
     */
    public DatabaseMetadataService getDatabaseMetadataService() {
        return databaseMetadataService;
    }


    /**
     * Execute artifact export.
     *
     * @param datasource the datasource
     * @param schema the schema name
     * @param structure the structure name
     * @return the response
     * @throws SQLException the SQL exception
     */
    @GetMapping(value = "/{datasource}/{schema}/{structure}", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> exportArtifact(@PathVariable("datasource") String datasource,
            @PathVariable("schema") String schema, @PathVariable("structure") String structure) throws SQLException {

        if (!databaseMetadataService.existsDataSourceMetadata(datasource)) {
            String error = format("Datasource {0} does not exist.", datasource);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        StreamingResponseBody responseBody = output -> {
            try {
                databaseExportService.exportStructure(datasource, schema, structure, output);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };

        String type = databaseExportService.structureExportType(datasource, schema, structure);

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION,
                                     "attachment; filename=\"" + schema + "." + structure + "-"
                                             + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + "." + type + "\"")
                             .contentType(MediaType.APPLICATION_OCTET_STREAM)
                             .body(responseBody);
    }

    /**
     * Execute schema export.
     *
     * @param datasource the datasource
     * @param schema the schema name
     * @return the response
     * @throws SQLException the SQL exception
     */
    @GetMapping(value = "/{datasource}/{schema}", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> exportSchema(@PathVariable("datasource") String datasource,
            @PathVariable("schema") String schema) throws SQLException {

        if (!databaseMetadataService.existsDataSourceMetadata(datasource)) {
            String error = format("Datasource {0} does not exist.", datasource);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        StreamingResponseBody responseBody = output -> {
            try {
                databaseExportService.exportSchema(datasource, schema, output);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION,
                                     "attachment; filename=\"" + schema + "-" + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
                                             + ".zip\"")
                             .contentType(MediaType.APPLICATION_OCTET_STREAM)
                             .body(responseBody);
    }
}
