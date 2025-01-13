/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.logs.endpoint;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.validation.Valid;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.logs.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class LoggersEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "loggers")
public class LogsConfigurationsEndpoint {

    /** The logs service. */
    @Autowired
    private LogsService logsService;

    /**
     * List all loggers with their severity level.
     *
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException the I/O error
     */
    @GetMapping(value = "/", produces = "application/json")
    public ResponseEntity<?> listLoggers() throws URISyntaxException, IOException {
        return ResponseEntity.ok(logsService.listLoggers());
    }

    /**
     * Get severity.
     *
     * @param loggerName the loggerName
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException the I/O error
     */
    @GetMapping(value = "/severity/{loggerName}", produces = "text/plain")
    public ResponseEntity<?> getSeverity(@PathVariable("loggerName") String loggerName) throws URISyntaxException, IOException {

        if (loggerName == null) {
            loggerName = "ROOT";
        }

        return ResponseEntity.ok(logsService.getSeverity(loggerName));
    }

    /**
     * Set severity.
     *
     * @param loggerName the loggerName
     * @param logLevel the logLevel
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException the I/O error
     */
    @PostMapping(value = "/severity/{loggerName}", produces = "text/plain", consumes = "text/plain")
    public ResponseEntity<?> setSeverity(@PathVariable("loggerName") String loggerName, @Valid @RequestBody String logLevel)
            throws URISyntaxException, IOException {

        if (loggerName == null) {
            loggerName = "ROOT";
        }

        return ResponseEntity.ok(logsService.setSeverity(loggerName, logLevel));
    }

}
