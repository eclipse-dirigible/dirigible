/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.command.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.command.service.CommandService;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class WikiEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "command"})
public class CommandEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CommandEndpoint.class.getCanonicalName());

    /** The command service. */
    private final CommandService commandService;

    /**
     * Instantiates a new command endpoint.
     *
     * @param commandService the command service
     */
    @Autowired
    public CommandEndpoint(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Gets the page.
     *
     * @param path the file path
     * @param params the params
     * @return the response
     * @throws Exception the exception
     */
    @GetMapping("/{*path}")
    public ResponseEntity<?> get(@PathVariable("path") String path,
            @Nullable @RequestParam(required = false) MultiValueMap<String, String> params) throws Exception {
        if (commandService.existResource(path)) {
            IResource resource = commandService.getResource(path);
            if (resource.isBinary()) {
                String message = "Resource found, but it is a binary file: " + path;
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }

            String result = commandService.executeCommand(path, params.toSingleValueMap());
            return ResponseEntity.ok(result);
        }

        String errorMessage = "Resource not found: " + path;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
    }

}
