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

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.logs.service.LogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class LogsEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "logs")
public class LogsEndpoint {

    /** The logs service. */
    @Autowired
    private LogsService logsService;

    /**
     * List all the log files in the logs folder.
     *
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException the I/O error
     */
    @GetMapping(value = "/", produces = "application/json")
    public ResponseEntity<?> listLogs() throws URISyntaxException, IOException {
        return ResponseEntity.ok(logsService.list());
    }

    /**
     * Search.
     *
     * @param file the file
     * @return the response
     * @throws URISyntaxException the URI syntax exception
     * @throws IOException the I/O error
     */
    @GetMapping(value = "/{file}", produces = "text/plain")
    public ResponseEntity<?> list(@PathVariable("file") String file) throws URISyntaxException, IOException {
        return ResponseEntity.ok(logsService.get(file));
    }

}
