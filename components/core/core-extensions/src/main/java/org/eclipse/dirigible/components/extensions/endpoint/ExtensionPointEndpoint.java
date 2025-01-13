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
import org.eclipse.dirigible.components.extensions.domain.ExtensionPoint;
import org.eclipse.dirigible.components.extensions.service.ExtensionPointService;
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
 * The Class ExtensionPointEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "extensionpoints")
public class ExtensionPointEndpoint extends BaseEndpoint {

    /** The extension point service. */
    private final ExtensionPointService extensionPointService;

    /** The extension service. */
    private final ExtensionService extensionService;

    /**
     * Instantiates a new extension point endpoint.
     *
     * @param extensionPointService the extension point service
     * @param extensionService the extension service
     */
    @Autowired
    public ExtensionPointEndpoint(ExtensionPointService extensionPointService, ExtensionService extensionService) {
        this.extensionPointService = extensionPointService;
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
    public Page<ExtensionPoint> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {

        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ExtensionPoint> extensionPoints = extensionPointService.getPages(pageable);
        return extensionPoints;

    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExtensionPoint> get(@PathVariable("id") Long id) {

        return ResponseEntity.ok(extensionPointService.findById(id));

    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<ExtensionPoint> findByName(@RequestParam("name") String name) {

        return ResponseEntity.ok(extensionPointService.findByName(name));

    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<ExtensionPoint>> getAll() {

        return ResponseEntity.ok(extensionPointService.getAll());

    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping("/tree")
    public ResponseEntity<List<ExtensionPoint>> getTree() {
        List<ExtensionPoint> list = extensionPointService.getAll();
        list.forEach(ep -> ep.getExtensions()
                             .addAll(extensionService.findByExtensionPoint(ep.getName())));
        return ResponseEntity.ok(list);

    }

}
