/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tracing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * The Class TaskStateEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "tracing")
public class TaskStateEndpoint extends BaseEndpoint {

    /** The task tracing service. */
    private final TaskStateService taskStateService;

    /**
     * Instantiates a new task tracing endpoint.
     *
     * @param taskStateService the task state service
     */
    @Autowired
    public TaskStateEndpoint(TaskStateService taskStateService) {
        this.taskStateService = taskStateService;
    }

    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<TaskState> findAll(
            @Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {
        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskState> taskStates = taskStateService.getPages(pageable);
        return taskStates;
    }

    /**
     * Gets the.
     *
     * @param id the id
     * @return the response entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskState> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(taskStateService.findById(id));
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<List<TaskState>> findByExecution(@RequestParam("execution") String execution) {
        return ResponseEntity.ok(taskStateService.findByExecution(execution));
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    @GetMapping
    public ResponseEntity<List<TaskState>> getAll() {
        return ResponseEntity.ok(taskStateService.getAll());
    }

    /**
     * Deletes all the tracing data.
     *
     * @return the response entity
     * @throws URISyntaxException the URI syntax exception
     */
    @DeleteMapping
    public ResponseEntity<URI> deleteAll() throws URISyntaxException {
        taskStateService.deleteAll();
        return ResponseEntity.noContent()
                             .build();
    }

}
