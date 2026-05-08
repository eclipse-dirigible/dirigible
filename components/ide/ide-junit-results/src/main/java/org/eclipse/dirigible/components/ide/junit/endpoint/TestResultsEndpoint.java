/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.junit.endpoint;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.junit.domain.TestResult;
import org.eclipse.dirigible.components.ide.junit.domain.TestResultsSummary;
import org.eclipse.dirigible.components.ide.junit.service.TestResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * REST endpoint for managing test results.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "junit-results")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
public class TestResultsEndpoint {

    @Autowired
    private TestResultsService testResultsService;

    /**
     * Get all test results.
     *
     * @return list of test results
     */
    @GetMapping
    public ResponseEntity<List<TestResult>> getResults() {
        return ResponseEntity.ok(testResultsService.getResults());
    }

    /**
     * Get test results summary.
     *
     * @return test results summary
     */
    @GetMapping("/summary")
    public ResponseEntity<TestResultsSummary> getSummary() {
        return ResponseEntity.ok(testResultsService.getSummary());
    }

    /**
     * Get test results filtered by status.
     *
     * @param status the status to filter by (passed, failed, skipped)
     * @return list of test results with the specified status
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<TestResult>> getResultsByStatus(@RequestParam String status) {
        return ResponseEntity.ok(testResultsService.getResultsByStatus(status));
    }

    /**
     * Store new test results.
     *
     * @param results the test results to store
     * @return response entity with status
     */
    @PostMapping
    public ResponseEntity<TestResultsSummary> storeResults(@RequestBody List<TestResult> results) {
        testResultsService.storeResults(results);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(testResultsService.getSummary());
    }

    /**
     * Clear all test results.
     *
     * @return response entity with status
     */
    @DeleteMapping
    public ResponseEntity<Void> clearResults() {
        testResultsService.clearResults();
        return ResponseEntity.noContent()
                             .build();
    }

}
