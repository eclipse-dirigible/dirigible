/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.perf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Counts the statements of every request that names its measured step in {@link #STEP_HEADER}, and
 * keeps one count per request under that step. Requests without the header - the setup, the
 * warm-up, the platform's own traffic - are not counted.
 */
class StatementCountingFilter extends OncePerRequestFilter {

    /** The request header naming the step a measured request belongs to. */
    static final String STEP_HEADER = "X-Perf-Step";

    private static final Map<String, Queue<Map<String, Integer>>> RECORDED = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String step = request.getHeader(STEP_HEADER);
        if (step == null) {
            chain.doFilter(request, response);
            return;
        }
        StatementCounter.begin();
        try {
            chain.doFilter(request, response);
        } finally {
            RECORDED.computeIfAbsent(step, name -> new ConcurrentLinkedQueue<>())
                    .add(StatementCounter.end());
        }
    }

    /**
     * The statements of each request recorded for a step so far. The count of a request lands once its
     * filter chain has returned, which can be a moment after its client has read the response.
     *
     * @param step the step
     * @return one map of statement counts by SQL per request, in no particular order
     */
    static List<Map<String, Integer>> recorded(String step) {
        return List.copyOf(RECORDED.getOrDefault(step, new ConcurrentLinkedQueue<>()));
    }
}
