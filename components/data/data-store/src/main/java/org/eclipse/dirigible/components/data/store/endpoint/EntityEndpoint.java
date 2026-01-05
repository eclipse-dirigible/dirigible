/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.store.domain.Entity;
import org.eclipse.dirigible.components.data.store.service.EntityService;
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
import jakarta.annotation.security.RolesAllowed;

/**
 * The Class EntityEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_DATA + "entities")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class EntityEndpoint extends BaseEndpoint {

    /** The entity service. */
    private final EntityService entityService;

    /**
     * Instantiates a new entity endpoint.
     *
     * @param entityService the entity service
     */
    @Autowired
    public EntityEndpoint(EntityService entityService) {
        this.entityService = entityService;
    }

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<Entity> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {
        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Entity> entities = entityService.getPages(pageable);
        return entities;
    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Entity> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(entityService.findById(id));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<Entity> findByName(@RequestParam("name") String name) {
        return ResponseEntity.ok(entityService.findByName(name));
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<Entity>> getAll() {
        return ResponseEntity.ok(entityService.getAll());
    }

}
