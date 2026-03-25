/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.endpoint;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.export.service.DataAsyncExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

/**
 * Front facing REST service serving the raw data.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "export-async")
@RolesAllowed({"ADMINISTRATOR", "OPERATOR"})
public class DataAsyncExportEndpoint {

    private final DataAsyncExportService dataAsyncExportService;



    /**
     * Instantiates a new database export endpoint.
     *
     * @param dataAsyncExportService the database async export service
     */
    @Autowired
    public DataAsyncExportEndpoint(DataAsyncExportService dataAsyncExportService) {
        this.dataAsyncExportService = dataAsyncExportService;
    }

    public DataAsyncExportService getDataAsyncExportService() {
        return dataAsyncExportService;
    }


    /**
     * Execute statement export.
     *
     * @param datasource the datasource
     * @param statement the statement
     * @param name the optional name parameter
     * @return the response
     * @throws SQLException the SQL exception
     */
    @PostMapping(value = "/{datasource}", produces = "application/octet-stream")
    public ResponseEntity<StreamingResponseBody> exportStatement(@PathVariable("datasource") String datasource,
            @Valid @RequestBody String statement, @RequestParam("name") Optional<String> name) throws SQLException {

        getDataAsyncExportService().exportStatement(datasource, statement, Optional.empty(), name);

        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Gets the export.
     *
     * @return the response
     */
    @GetMapping("/")
    public ResponseEntity<String> get() {
        try {
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(GsonHelper.toJson(getDataAsyncExportService().get()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Deletes all the exports.
     *
     * @return the response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {

        try {
            getDataAsyncExportService().delete(id);

            return ResponseEntity.noContent()
                                 .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Deletes the export.
     *
     * @return the response
     */
    @DeleteMapping("/")
    public ResponseEntity<String> deleteAll() {

        try {
            getDataAsyncExportService().deleteAll();

            return ResponseEntity.noContent()
                                 .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }



}
