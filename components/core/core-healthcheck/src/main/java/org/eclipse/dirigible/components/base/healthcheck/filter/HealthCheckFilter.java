/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.dirigible.components.base.healthcheck.status.HealthCheckStatus;
import org.springframework.stereotype.Component;

/**
 * The HTTP Context Filter.
 */
@Component
public class HealthCheckFilter implements Filter {

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
        String path = getRequestPath(httpRequest);
        boolean isResources = isResourcesRequest(path);
        boolean isHealthCheck = isHealtCheckRequest(path);
        boolean isOps = isOpsRequest(path);
        boolean isWebJars = isWebJarsRequest(path);
        boolean isTheme = isThemeRequest(path);
        if (!isResources && !isHealthCheck && !isOps && !isWebJars && !isTheme) {
            HealthCheckStatus healthStatus = HealthCheckStatus.getInstance();
            if (healthStatus.getStatus()
                            .equals(HealthCheckStatus.Status.Ready)) {
                chain.doFilter(request, response);
                return;
            }
            httpResponse.sendRedirect("/index-busy.html");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Checks if is resources request.
     *
     * @param path the path
     * @return true, if is resources request
     */
    private boolean isResourcesRequest(String path) {
        return path.startsWith("/web/resources") || path.startsWith("/js/resources");
    }

    /**
     * Checks if is healt check request.
     *
     * @param path the path
     * @return true, if is healt check request
     */
    private boolean isHealtCheckRequest(String path) {
        return path.startsWith("/core/healthcheck") || path.startsWith("/index-busy.html");
    }

    /**
     * Checks if is ops request.
     *
     * @param path the path
     * @return true, if is ops request
     */
    private boolean isOpsRequest(String path) {
        return path.startsWith("/ops");
    }

    /**
     * Checks if is web jars request.
     *
     * @param path the path
     * @return true, if is web jars request
     */
    private boolean isWebJarsRequest(String path) {
        return path.startsWith("/webjars");
    }

    /**
     * Checks if is theme request.
     *
     * @param path the path
     * @return true, if is theme request
     */
    private boolean isThemeRequest(String path) {
        return path.startsWith("/web/theme/") || path.startsWith("/js/theme/");
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

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        // Not used
    }

}
