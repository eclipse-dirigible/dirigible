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

import java.util.List;

import org.eclipse.dirigible.components.ide.junit.domain.TestResult;
import org.eclipse.dirigible.components.ide.junit.domain.TestResultsSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Global service facade for accessing test results. This can be used to programmatically retrieve
 * test results from Java code.
 */
@Component
public class TestResultsFacade {

    @Autowired
    private TestResultsService testResultsService;

    /**
     * Get all test results collected in the current session.
     *
     * @return list of test results
     */
    public List<TestResult> getAllResults() {
        return testResultsService.getResults();
    }

    /**
     * Get a summary of test execution statistics.
     *
     * @return test results summary with counts and duration
     */
    public TestResultsSummary getResultsSummary() {
        return testResultsService.getSummary();
    }

    /**
     * Get test results filtered by status.
     *
     * @param status the status to filter by (e.g., "passed", "failed", "skipped")
     * @return filtered list of test results
     */
    public List<TestResult> getResultsByStatus(String status) {
        return testResultsService.getResultsByStatus(status);
    }

    /**
     * Store test results from a test execution.
     *
     * @param results the test results to store
     */
    public void storeTestResults(List<TestResult> results) {
        testResultsService.storeResults(results);
    }

    /**
     * Add a single test result.
     *
     * @param result the test result to add
     */
    public void addTestResult(TestResult result) {
        testResultsService.addResult(result);
    }

    /**
     * Clear all stored test results.
     */
    public void clearAllResults() {
        testResultsService.clearResults();
    }

    /**
     * Check if there are any test failures.
     *
     * @return true if there are failed tests, false otherwise
     */
    public boolean hasFailures() {
        TestResultsSummary summary = getResultsSummary();
        return summary.getFailed() > 0;
    }

    /**
     * Check if all tests passed.
     *
     * @return true if all tests passed (or no tests), false if any failed
     */
    public boolean allTestsPassed() {
        return !hasFailures();
    }

    /**
     * Get the count of passed tests.
     *
     * @return number of passed tests
     */
    public int getPassedCount() {
        return getResultsSummary().getPassed();
    }

    /**
     * Get the count of failed tests.
     *
     * @return number of failed tests
     */
    public int getFailedCount() {
        return getResultsSummary().getFailed();
    }

    /**
     * Get the count of skipped tests.
     *
     * @return number of skipped tests
     */
    public int getSkippedCount() {
        return getResultsSummary().getSkipped();
    }

    /**
     * Get the total count of tests.
     *
     * @return total number of tests
     */
    public int getTotalTestCount() {
        return getResultsSummary().getTotal();
    }

    /**
     * Get the total execution duration in milliseconds.
     *
     * @return total execution duration
     */
    public long getTotalDuration() {
        return getResultsSummary().getDuration();
    }

}
