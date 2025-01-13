/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.endpoint;

import static org.eclipse.dirigible.components.base.endpoint.BaseEndpoint.DEFAULT_PAGE_SIZE;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.service.ListenerService;
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
 * The Class ListenerEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "listeners")
public class ListenerEndpoint {

    /** The listener service. */
    @Autowired
    private ListenerService listenerService;

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<Listener> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {

        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Listener> listeners = listenerService.getPages(pageable);
        return listeners;
    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Listener> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(listenerService.findById(id));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<Listener> findByName(@RequestParam("name") String name) {

        return ResponseEntity.ok(listenerService.findByName(name));

    }

    /**
     * List jobs.
     *
     * @return the response entity
     */
    @GetMapping()
    public ResponseEntity<List<Listener>> listJobs() {
        return ResponseEntity.ok(listenerService.getAll());
    }
}
