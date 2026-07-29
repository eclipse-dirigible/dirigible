/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * The boot-only traffic gate (#6448).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealthCheckFilterTest {

    private static final String APP_PATH = "/services/web/myapp/index.html";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private final HealthCheckFilter filter = new HealthCheckFilter();

    @BeforeEach
    void resetReadiness() {
        PlatformReadiness.getInstance()
                         .reset();
    }

    @AfterEach
    void releaseTheGate() {
        // The singleton outlives the test - leave it open so unrelated tests are unaffected.
        PlatformReadiness.getInstance()
                         .passCompleted(0);
    }

    @Test
    void aBootedInstancePassesEverythingThrough() throws Exception {
        PlatformReadiness.getInstance()
                         .passCompleted(0);
        givenServletPath(APP_PATH);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    void anApiClientIsRefusedWithARetryableUnavailableDuringBoot() throws Exception {
        givenServletPath(APP_PATH);
        when(request.getHeader("Accept")).thenReturn("application/json");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setHeader("Retry-After", "5");
    }

    @Test
    void aBrowserIsSentToTheBusyPageDuringBoot() throws Exception {
        givenServletPath(APP_PATH);
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).sendRedirect("/index-busy.html");
        verify(response, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    /**
     * The IDE, authentication, the actuator and the platform's own status surfaces have to answer while
     * the instance boots - that is how an operator finds out what it is waiting for.
     */
    @Test
    void thePathsNeededToDiagnoseBootAreNeverGated() throws Exception {
        for (String path : new String[] {"/services/web/shell-ide/index.html", "/services/core/readiness",
                "/services/core/healthcheck/status", "/actuator/health/readiness", "/login", "/webjars/alpinejs/dist/cdn.min.js",
                "/services/web/platform-core/ui/styles/fonts.css", "/index-busy.html"}) {
            FilterChain freshChain = org.mockito.Mockito.mock(FilterChain.class);
            HttpServletRequest freshRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
            when(freshRequest.getPathInfo()).thenReturn(null);
            when(freshRequest.getServletPath()).thenReturn(path);

            filter.doFilter(freshRequest, response, freshChain);

            verify(freshChain, org.mockito.Mockito.description(path + " must be served during boot")).doFilter(freshRequest, response);
        }
        verify(response, never()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response, never()).sendRedirect(anyString());
    }

    private void givenServletPath(String path) {
        when(request.getPathInfo()).thenReturn(null);
        when(request.getServletPath()).thenReturn(path);
    }
}
