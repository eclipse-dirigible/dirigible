/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import java.io.IOException;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Loads the current tenant's overridable configuration into the thread-scoped configuration for the
 * duration of the request, so that {@link Configuration#get(String)} resolves tenant-specific
 * values with precedence over the environment and property sources.
 * <p>
 * Registered at the lowest precedence, so it runs downstream of the Spring Security filter chain -
 * and therefore inside the tenant execution scope established by the tenant context filter. It is
 * intentionally kept in this module (rather than editing the tenant context filter) so that
 * core-tenants does not have to depend on core-configurations.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantConfigurationInitFilter extends OncePerRequestFilter {

    private final TenantConfigurationService tenantConfigurationService;

    public TenantConfigurationInitFilter(TenantConfigurationService tenantConfigurationService) {
        this.tenantConfigurationService = tenantConfigurationService;
    }

    /**
     * Do filter internal.
     *
     * @param request the request
     * @param response the response
     * @param chain the filter chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> injectable = tenantConfigurationService.resolveInjectableForCurrentTenant();
        // Clears any previous thread configuration when empty, so pooled threads never leak values.
        Configuration.setThreadConfiguration(injectable);
        try {
            chain.doFilter(request, response);
        } finally {
            Configuration.removeThreadConfiguration();
        }
    }

}
