/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.junit.service;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.ide.junit.domain.TestResult;
import org.eclipse.dirigible.components.ide.junit.domain.TestResultsSummary;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Service for managing test results. Note: This is an in-memory implementation. Consider adding
 * database persistence for production use.
 */
@Service
public class TestResultsService implements InitializingBean {

    private List<TestResult> currentResults = new ArrayList<>();

    /** The test results service instance. */
    private static TestResultsService INSTANCE;

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the test results service instance
     */
    public static TestResultsService get() {
        return INSTANCE;
    }

    /**
     * Store test results from a test run.
     *
     * @param results the list of test results to store
     */
    public void storeResults(List<TestResult> results) {
        this.currentResults = new ArrayList<>(results);
    }

    /**
     * Add a single test result.
     *
     * @param result the test result to add
     */
    public void addResult(TestResult result) {
        this.currentResults.add(result);
    }

    /**
     * Get all stored test results.
     *
     * @return list of test results
     */
    public List<TestResult> getResults() {
        return new ArrayList<>(currentResults);
    }

    /**
     * Get a summary of test results.
     *
     * @return test results summary
     */
    public TestResultsSummary getSummary() {
        TestResultsSummary summary = new TestResultsSummary();

        for (TestResult result : currentResults) {
            summary.setTotal(summary.getTotal() + 1);
            summary.setDuration(summary.getDuration() + result.getDuration());

            if ("passed".equalsIgnoreCase(result.getStatus())) {
                summary.setPassed(summary.getPassed() + 1);
            } else if ("failed".equalsIgnoreCase(result.getStatus())) {
                summary.setFailed(summary.getFailed() + 1);
            } else if ("skipped".equalsIgnoreCase(result.getStatus())) {
                summary.setSkipped(summary.getSkipped() + 1);
            }
        }

        return summary;
    }

    /**
     * Clear all stored test results.
     */
    public void clearResults() {
        this.currentResults.clear();
    }

    /**
     * Get test results filtered by status.
     *
     * @param status the status to filter by
     * @return list of test results with the specified status
     */
    public List<TestResult> getResultsByStatus(String status) {
        List<TestResult> filtered = new ArrayList<>();
        for (TestResult result : currentResults) {
            if (status.equalsIgnoreCase(result.getStatus())) {
                filtered.add(result);
            }
        }
        return filtered;
    }

}
