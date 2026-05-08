/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */

const API_BASE_URL = '/services/ide/junit-results';
let currentFilter = 'all';
let allResults = [];
let currentSummary = null;

/**
 * Initialize the UI on page load
 */
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadResults();
    // Auto-refresh every 5 seconds
    setInterval(loadResults, 5000);
});

/**
 * Setup event listeners for buttons and controls
 */
function setupEventListeners() {
    const refreshBtn = document.getElementById('refreshBtn');
    const clearBtn = document.getElementById('clearBtn');

    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadResults);
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', clearResults);
    }
}

/**
 * Load test results from the API
 */
async function loadResults() {
    const loadingIndicator = document.getElementById('loadingIndicator');
    const resultsList = document.getElementById('resultsList');
    const emptyState = document.getElementById('emptyState');

    try {
        // Show loading state
        loadingIndicator.style.display = 'flex';
        resultsList.innerHTML = '';
        emptyState.style.display = 'none';

        // Fetch results and summary
        const [resultsResponse, summaryResponse] = await Promise.all([
            fetch(`${API_BASE_URL}`),
            fetch(`${API_BASE_URL}/summary`)
        ]);

        if (!resultsResponse.ok || !summaryResponse.ok) {
            throw new Error('Failed to load test results');
        }

        allResults = await resultsResponse.json();
        currentSummary = await summaryResponse.json();

        // Update UI
        updateSummary();
        updateStatusBar();
        filterResults(currentFilter);

        // Hide loading indicator
        loadingIndicator.style.display = 'none';

        // Show empty state if no results
        if (allResults.length === 0) {
            emptyState.style.display = 'flex';
        }

    } catch (error) {
        console.error('Error loading test results:', error);
        loadingIndicator.style.display = 'none';
        resultsList.innerHTML = `<div class="error-message">Failed to load test results: ${error.message}</div>`;
    }
}

/**
 * Update the summary panel with test counts
 */
function updateSummary() {
    if (!currentSummary) return;

    document.getElementById('totalTests').textContent = currentSummary.total;
    document.getElementById('passedTests').textContent = currentSummary.passed;
    document.getElementById('failedTests').textContent = currentSummary.failed;
    document.getElementById('skippedTests').textContent = currentSummary.skipped;

    // Format duration
    const durationMs = currentSummary.duration;
    const durationText = durationMs < 1000
        ? `${durationMs} ms`
        : `${(durationMs / 1000).toFixed(2)} s`;
    document.getElementById('totalDuration').textContent = durationText;
}

/**
 * Update the status bar based on test results
 */
function updateStatusBar() {
    const statusBar = document.getElementById('statusBar');

    if (!currentSummary) {
        statusBar.className = 'status-bar';
        return;
    }

    if (currentSummary.total === 0) {
        statusBar.className = 'status-bar';
    } else if (currentSummary.failed === 0) {
        statusBar.className = 'status-bar all-passed';
    } else {
        statusBar.className = 'status-bar has-failed';
    }
}

/**
 * Filter results by status
 */
function filterResults(filter) {
    currentFilter = filter;

    // Update active tab
    document.querySelectorAll('.filter-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.dataset.filter === filter) {
            tab.classList.add('active');
        }
    });

    // Filter and display results
    let filteredResults = allResults;
    if (filter !== 'all') {
        filteredResults = allResults.filter(result => result.status.toLowerCase() === filter.toLowerCase());
    }

    displayResults(filteredResults);
}

/**
 * Display test results in the list
 */
function displayResults(results) {
    const resultsList = document.getElementById('resultsList');
    const emptyState = document.getElementById('emptyState');

    if (results.length === 0) {
        resultsList.innerHTML = '';
        emptyState.style.display = 'flex';
        return;
    }

    emptyState.style.display = 'none';
    resultsList.innerHTML = '';

    results.forEach((result, index) => {
        const item = createResultItem(result, index);
        resultsList.appendChild(item);
    });
}

/**
 * Create a single result item element
 */
function createResultItem(result, index) {
    const item = document.createElement('div');
    item.className = 'test-result-item';
    item.dataset.testIndex = index;

    // Determine icon and status color
    const { icon, statusClass } = getStatusInfo(result.status);

    // Build HTML
    const hasError = result.error && result.stackTrace;
    const errorHtml = hasError
        ? `<div class="test-result-error"><strong>Error:</strong> ${result.error}\n\n<strong>Stack Trace:</strong>\n${escapeHtml(result.stackTrace)}</div>`
        : '';

    const durationText = result.duration < 1000
        ? `${result.duration} ms`
        : `${(result.duration / 1000).toFixed(2)} s`;

    item.innerHTML = `
        <div class="test-result-icon">${icon}</div>
        <div class="test-result-content">
            <div class="test-result-name">${escapeHtml(result.name)}</div>
            <div class="test-result-details">
                <span class="test-result-status ${statusClass}">${result.status.toUpperCase()}</span>
                <span>Duration: ${durationText}</span>
                <span>at ${new Date(result.timestamp).toLocaleTimeString()}</span>
            </div>
            ${errorHtml}
        </div>
    `;

    // Add click handler to expand/collapse error details
    if (hasError) {
        item.addEventListener('click', function(e) {
            if (!e.target.closest('.test-result-error a')) {
                this.classList.toggle('expanded');
            }
        });
    }

    return item;
}

/**
 * Get status icon and class based on test status
 */
function getStatusInfo(status) {
    const statusLower = status.toLowerCase();

    const statusMap = {
        passed: { icon: '✓', statusClass: 'status-passed' },
        failed: { icon: '✗', statusClass: 'status-failed' },
        skipped: { icon: '—', statusClass: 'status-skipped' }
    };

    return statusMap[statusLower] || { icon: '?', statusClass: 'status-unknown' };
}

/**
 * Clear all test results
 */
async function clearResults() {
    if (!confirm('Are you sure you want to clear all test results?')) {
        return;
    }

    try {
        const response = await fetch(API_BASE_URL, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Failed to clear test results');
        }

        allResults = [];
        currentSummary = null;
        loadResults();

    } catch (error) {
        console.error('Error clearing test results:', error);
        alert('Failed to clear test results: ' + error.message);
    }
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Format duration in milliseconds to a readable string
 */
function formatDuration(ms) {
    if (ms < 1000) return `${ms} ms`;
    return `${(ms / 1000).toFixed(2)} s`;
}
