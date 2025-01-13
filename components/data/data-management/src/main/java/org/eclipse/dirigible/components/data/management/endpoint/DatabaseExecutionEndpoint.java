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

import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.management.service.DatabaseExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

/**
 * The Class DatabaseExecutionEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA)
public class DatabaseExecutionEndpoint {

    /** The database execution service. */
    private final DatabaseExecutionService databaseExecutionService;

    /**
     * Instantiates a new data source metadata endpoint.
     *
     * @param databaseExecutionService the database execution service
     */
    @Autowired
    public DatabaseExecutionEndpoint(DatabaseExecutionService databaseExecutionService) {
        this.databaseExecutionService = databaseExecutionService;
    }

    /**
     * Gets the supported message.
     *
     * @return the message
     */
    @GetMapping(value = "/", produces = "application/json")
    public ResponseEntity<String> getDatabases() {
        return ResponseEntity.ok(GsonHelper.toJson(List.of("metadata")));
    }

    /**
     * Query statement.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param accept the accept
     * @return the data sources
     * @throws Exception the exception
     */
    @PostMapping(value = "/{datasource}/query", consumes = "text/plain")
    public ResponseEntity<StreamingResponseBody> executeQuery(@PathVariable("datasource") String datasource, @RequestBody String sql,
            @RequestHeader(HttpHeaders.ACCEPT) String accept) throws Exception {

        StreamingResponseBody responseBody = output -> {
            try {
                if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
                    databaseExecutionService.executeQuery(datasource, sql, false, false, output);
                } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
                    databaseExecutionService.executeQuery(datasource, sql, false, true, output);
                } else {
                    databaseExecutionService.executeQuery(datasource, sql, true, false, output);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        };

        if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else {
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(responseBody);
        }
    }

    /**
     * Update statement.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param accept the accept
     * @return the data sources
     * @throws Exception the exception
     */
    @PostMapping(value = "/{datasource}/update", consumes = "text/plain")
    public ResponseEntity<StreamingResponseBody> executeUpdate(@PathVariable("datasource") String datasource, @RequestBody String sql,
            @RequestHeader(HttpHeaders.ACCEPT) String accept) throws Exception {

        StreamingResponseBody responseBody = output -> {
            try {
                if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
                    databaseExecutionService.executeUpdate(datasource, sql, false, false, output);
                } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
                    databaseExecutionService.executeUpdate(datasource, sql, false, true, output);
                } else {
                    databaseExecutionService.executeUpdate(datasource, sql, true, false, output);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        };

        if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else {
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(responseBody);
        }
    }

    /**
     * Update statement.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param accept the accept
     * @return the data sources
     * @throws Exception the exception
     */
    @PostMapping(value = "/{datasource}/procedure", consumes = "text/plain")
    public ResponseEntity<StreamingResponseBody> executeProcedure(@PathVariable("datasource") String datasource, @RequestBody String sql,
            @RequestHeader(HttpHeaders.ACCEPT) String accept) throws Exception {

        StreamingResponseBody responseBody = output -> {
            try {
                if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
                    databaseExecutionService.executeProcedure(datasource, sql, false, false, output);
                } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
                    databaseExecutionService.executeProcedure(datasource, sql, false, true, output);
                } else {
                    databaseExecutionService.executeProcedure(datasource, sql, true, false, output);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        };

        if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else {
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(responseBody);
        }
    }

    /**
     * Execute statement.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param accept the accept
     * @return the data sources
     * @throws Exception the exception
     */
    @PostMapping(value = "/{datasource}/execute", consumes = "text/plain")
    public ResponseEntity<StreamingResponseBody> execute(@PathVariable("datasource") String datasource, @RequestBody String sql,
            @RequestHeader(HttpHeaders.ACCEPT) String accept) throws Exception {

        StreamingResponseBody responseBody = output -> {
            try {
                if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
                    databaseExecutionService.execute(datasource, sql, false, false, output);
                } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
                    databaseExecutionService.execute(datasource, sql, false, true, output);
                } else {
                    databaseExecutionService.execute(datasource, sql, true, false, output);
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        };

        if (ContentTypeHelper.TEXT_PLAIN.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else if (ContentTypeHelper.TEXT_CSV.equals(accept)) {
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_PLAIN)
                                 .body(responseBody);
        } else {
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(responseBody);
        }
    }

}
