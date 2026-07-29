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

import java.io.IOException;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * The boot-only traffic gate (#6448): while the first synchronization pass has not depleted its
 * artefact queue, application-facing requests are refused, because they would otherwise hit
 * half-initialized state (missing artefacts, unregistered controllers, unimported CSVIM data).
 *
 * <p>
 * Keyed to {@link PlatformReadiness#isBootCompleted()} - a ONE-WAY latch, so a later publish never
 * takes a running application offline, however long its pass runs. A browser is sent to the
 * auto-refreshing busy page it has always been sent to; every other client now gets a
 * {@code 503 Service Unavailable} with {@code Retry-After}, which is what an API client, a CI
 * pipeline or a probe can actually act on.
 *
 * <p>
 * {@link #EXCLUDED_PREFIXES} is the set of paths that must keep working while the platform boots -
 * the IDE (so a developer can watch the instance come up), authentication, the health and readiness
 * endpoints, the actuator, and static resources.
 */
@Component
public class HealthCheckFilter implements Filter {

    /**
     * Paths served during boot. Matched against the servlet path with the {@code /services} and
     * {@code /public} prefixes stripped (see {@link #getRequestPath(HttpServletRequest)}), so an IDE
     * module at {@code /services/web/shell-ide/...} is matched as {@code /web/shell-ide/...}.
     */
    private static final List<String> EXCLUDED_PREFIXES = List.of( //
            // Static and shared resources.
            "/web/resources", "/js/resources", "/webjars", "/web/theme/", "/js/theme/", //
            // The platform's own status surfaces and the busy page itself.
            "/core/healthcheck", "/core/readiness", "/index-busy.html", "/ops", "/actuator", "/error", //
            // Authentication - a user must be able to reach the IDE while the instance boots.
            "/login", "/logout", "/oauth2", "/saml2", //
            // The IDE shell and the modules it loads.
            "/web/shell-ide", "/web/platform-core", "/js/platform-core", "/web/ide-", "/web/editor-", "/web/view-", "/web/perspective-",
            "/web/menu-", "/web/service-", "/ide/", "/websockets/ide");

    /**
     * Inits the.
     *
     * @param filterConfig the filter config
     * @throws ServletException the servlet exception
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Not used
    }

    /**
     * Do filter.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServletException the servlet exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (PlatformReadiness.getInstance()
                             .isBootCompleted()
                || isExcluded(getRequestPath(httpRequest))) {
            chain.doFilter(request, response);
            return;
        }
        refuse(httpRequest, httpResponse);
    }

    /**
     * Refuses the request: the auto-refreshing busy page for a browser, a retryable 503 for every other
     * client.
     *
     * @param httpRequest the request
     * @param httpResponse the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void refuse(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        if (acceptsHtml(httpRequest)) {
            httpResponse.sendRedirect("/index-busy.html");
            return;
        }
        httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        httpResponse.setHeader(HttpHeaders.RETRY_AFTER, Integer.toString(DirigibleConfig.READINESS_GATE_RETRY_AFTER_SECONDS.getIntValue()));
    }

    /**
     * Whether the caller wants a page rather than data. A browser navigation sends
     * {@code Accept: text/html,...}; an API client, a probe or a {@code fetch()} does not.
     *
     * @param httpRequest the request
     * @return true when the caller accepts HTML
     */
    private boolean acceptsHtml(HttpServletRequest httpRequest) {
        String accept = httpRequest.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains("text/html");
    }

    /**
     * Checks whether the path must be served while the platform boots.
     *
     * @param path the path
     * @return true, if the path is excluded from the gate
     */
    private boolean isExcluded(String path) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the request path.
     *
     * @param httpRequest the http request
     * @return the request path
     */
    private String getRequestPath(HttpServletRequest httpRequest) {
        String path = httpRequest.getPathInfo();
        if (path == null) {
            path = httpRequest.getServletPath();
            path = path.replace("/services", "")
                       .replace("/public", "");
        }
        return path;
    }

}
