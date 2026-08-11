/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.filter;

import java.io.IOException;

import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reports a publish / unpublish request to the {@link RegistryMutationTracker} for as long as it is
 * being served, so the synchronization reconciler does not mistake the gap between the delete and
 * the copy of a replaced collection for a deletion.
 *
 * <p>
 * Only mutating methods count: the IDE polls these endpoints with GET all the time, and counting
 * those would keep the registry permanently "in mutation".
 */
@Component
class RegistryMutationFilter extends OncePerRequestFilter {

    private final RegistryMutationTracker tracker;

    RegistryMutationFilter(RegistryMutationTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        tracker.enter();
        try {
            chain.doFilter(request, response);
        } finally {
            tracker.exit();
        }
    }

    /**
     * Should not filter.
     *
     * @param request the request
     * @return true for requests that cannot mutate the registry
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod());
    }

}
