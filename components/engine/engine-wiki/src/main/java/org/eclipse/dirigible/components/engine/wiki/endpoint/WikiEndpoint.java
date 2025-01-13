/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.wiki.endpoint;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.wiki.service.WikiService;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class WikiEndpoint.
 */
@RestController
@RequestMapping({BaseEndpoint.PREFIX_ENDPOINT_SECURED + "wiki"})
public class WikiEndpoint extends BaseEndpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WikiEndpoint.class.getCanonicalName());


    /** The javascript service. */
    private final WikiService wikiService;

    /**
     * Instantiates a new wiki endpoint.
     *
     * @param wikiService the wiki service
     */
    @Autowired
    public WikiEndpoint(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    /**
     * Gets the page.
     *
     * @param path the file path
     * @return the response
     */
    @GetMapping("/{*path}")
    public ResponseEntity<?> get(@PathVariable("path") String path) {
        if (wikiService.existResource(path)) {
            IResource resource = wikiService.getResource(path);
            if (resource.isBinary()) {
                String message = "Resource found, but it is a binary file: " + path;
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
            String content = new String(resource.getContent(), StandardCharsets.UTF_8);
            String html = wikiService.renderContent(path, content);
            return ResponseEntity.ok(html);
        }

        String errorMessage = "Resource not found: " + path;
        try {
            byte[] content = wikiService.getResourceContent(path);
            if (content != null) {
                String html = wikiService.renderContent(path, new String(content, StandardCharsets.UTF_8));
                return ResponseEntity.ok(html);
            }
        } catch (RepositoryNotFoundException e) {
            throw new RepositoryNotFoundException(errorMessage, e);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage);
    }

}
