/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.problems.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.problems.domain.Problem;
import org.eclipse.dirigible.components.ide.problems.domain.Problems;
import org.eclipse.dirigible.components.ide.problems.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Class ProblemEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "problems")
public class ProblemEndpoint extends BaseEndpoint {

    /** The problem service. */
    private final ProblemService problemService;

    /**
     * Instantiates a new problem endpoint.
     *
     * @param problemService the problem service
     */
    @Autowired
    public ProblemEndpoint(ProblemService problemService) {
        this.problemService = problemService;
    }

    /**
     * List problems.
     *
     * @return the response entity
     * @throws Exception the exception
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<Problem>> listProblems() throws Exception {
        return ResponseEntity.ok(problemService.getAll());
    }

    /**
     * List all the problems currently registered.
     *
     * @param condition the condition
     * @param limit the limit
     * @return the response
     * @throws Exception the scheduler exception
     */
    @GetMapping(value = "/search", produces = "application/json")
    public ResponseEntity<Problems> fetchProblemsBatch(@Validated @RequestParam("condition") String condition,
            @Validated @RequestParam("limit") int limit) throws Exception {
        return ResponseEntity.ok(problemService.fetchProblemsBatch(condition, limit));
    }

    /**
     * Updates the status of all selected problems.
     *
     * @param status the status
     * @param selectedIds the selected ids
     * @return the complete list of problems after the update
     * @throws Exception the scheduler exception
     */
    @PostMapping(value = "/update/{status}", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> updateStatus(@Validated @PathVariable("status") String status,
            @RequestBody @Nullable ArrayList<Long> selectedIds) throws Exception {
        problemService.updateStatusByIds(selectedIds, status);
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Deletes all problems by their status.
     *
     * @param status the status
     * @return the response
     * @throws Exception the scheduler exception
     */
    @DeleteMapping(value = "/delete/{status}", produces = "application/json")
    public ResponseEntity<?> deleteProblemsByStatus(@Validated @PathVariable("status") String status) throws Exception {
        problemService.deleteAllByStatus(status);
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Deletes all problems. s
     *
     * @return the response
     * @throws Exception the scheduler exception
     */
    @DeleteMapping(value = "/clear", produces = "application/json")
    public ResponseEntity<?> clearProblems() throws Exception {
        problemService.deleteAll();
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Deletes all selected problems.
     *
     * @param selectedIds the selected ids
     * @return the response
     * @throws Exception the scheduler exception
     */
    @PostMapping(value = "/delete/selected", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> deleteMultipleProblems(@RequestBody @Nullable ArrayList<Long> selectedIds) throws Exception {
        problemService.deleteAllByIds(selectedIds);
        return ResponseEntity.noContent()
                             .build();
    }

}
