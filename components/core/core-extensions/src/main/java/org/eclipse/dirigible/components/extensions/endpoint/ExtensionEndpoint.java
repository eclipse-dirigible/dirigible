/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.extensions.domain.Extension;
import org.eclipse.dirigible.components.extensions.service.ExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The Class ExtensionEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "extensions")
public class ExtensionEndpoint extends BaseEndpoint {

    /** The extension service. */
    private final ExtensionService extensionService;

    /**
     * Instantiates a new extension endpoint.
     *
     * @param extensionService the extension service
     */
    @Autowired
    public ExtensionEndpoint(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<Extension> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {

        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Extension> extensions = extensionService.getPages(pageable);
        return extensions;

    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Extension> get(@PathVariable("id") Long id) {

        return ResponseEntity.ok(extensionService.findById(id));

    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search/{name}")
    public ResponseEntity<Extension> findByName(@RequestParam("name") String name) {

        return ResponseEntity.ok(extensionService.findByName(name));

    }

    /**
     * Find by extension point.
     *
     * @param name the name of the extension point
     * @return the response entity
     */
    @GetMapping("/group/{name}")
    public ResponseEntity<List<Extension>> findByExtensionPoint(@RequestParam("name") String name) {

        return ResponseEntity.ok(extensionService.findByExtensionPoint(name));

    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<Extension>> getAll() {

        return ResponseEntity.ok(extensionService.getAll());

    }

}
