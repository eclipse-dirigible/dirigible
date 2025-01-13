/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.anonymize.endpoint;

import static java.text.MessageFormat.format;

import java.sql.SQLException;

import jakarta.validation.Valid;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.anonymize.domain.DataAnonymizeParameters;
import org.eclipse.dirigible.components.data.anonymize.service.DataAnonymizeService;
import org.eclipse.dirigible.components.data.management.service.DatabaseMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Front facing REST service serving anymization functions.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "anonymize")
public class DataAnonymizeEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataAnonymizeEndpoint.class);

    /** The data anonymize service. */
    private DataAnonymizeService dataAnonymizeService;

    /** The database metadata service. */
    private DatabaseMetadataService databaseMetadataService;

    /**
     * Instantiates a new data anonymize endpoint.
     *
     * @param dataAnonymizeService the database anonymize service
     * @param databaseMetadataService the database metadata service
     */
    @Autowired
    public DataAnonymizeEndpoint(DataAnonymizeService dataAnonymizeService, DatabaseMetadataService databaseMetadataService) {
        this.dataAnonymizeService = dataAnonymizeService;
        this.databaseMetadataService = databaseMetadataService;
    }

    /**
     * Gets the data export anonymize service.
     *
     * @return the data export anonymize service
     */
    public DataAnonymizeService getDataAnonymizeService() {
        return dataAnonymizeService;
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
     * @param content the content
     * @return the response
     * @throws SQLException the SQL exception
     */
    @PostMapping(value = "column", produces = "application/json")
    public ResponseEntity anonymizeColumn(@Valid @RequestBody DataAnonymizeParameters content) throws SQLException {

        if (!databaseMetadataService.existsDataSourceMetadata(content.getDatasource())) {
            String error = format("Datasource {0} does not exist.", content.getDatasource());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }

        dataAnonymizeService.anonymizeColumn(content.getDatasource(), content.getSchema(), content.getTable(), content.getColumn(),
                content.getPrimaryKey(), content.getType());

        return ResponseEntity.ok()
                             .build();
    }

}
