/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Class TenantContextInitFilter.
 */
@Component
public class TenantContextInitFilter extends OncePerRequestFilter {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantContextInitFilter.class);

    private static final List<String> HOST_HEADERS = List.of("host", "x-forwarded-host");

    /** The tenant service. */
    private final TenantExtractor tenantExtractor;

    /** The tenant context. */
    private final TenantContext tenantContext;
    private final Gson gson;

    /**
     * Instantiates a new tenant context init filter.
     *
     * @param tenantExtractor the tenant service
     * @param tenantContext the tenant context
     */
    public TenantContextInitFilter(TenantExtractor tenantExtractor, TenantContext tenantContext) {
        this.tenantExtractor = tenantExtractor;
        this.tenantContext = tenantContext;
        this.gson = new GsonBuilder().serializeNulls()
                                     .create();
    }

    /**
     * Do filter internal.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Tenant> currentTenant = tenantExtractor.determineTenantSubdomain(request);
        if (currentTenant.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "There is no registered tenant for the current host");
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Tried to reach unregistered tenant. Headers: [{}]", getHeaders(request));
            }
            return;
        }

        try {
            tenantContext.execute(currentTenant.get(), () -> {
                chain.doFilter(request, response);
                return null;
            });

        } catch (ServletException | IOException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    private String getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        HOST_HEADERS.forEach(h -> headers.put(h, request.getHeader(h)));
        return gson.toJson(headers);
    }

    /**
     * Should not filter.
     *
     * @param request the request
     * @return true, if successful
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI()
                      .startsWith("/webjars/")
                || request.getRequestURI()
                          .startsWith("/css/")
                || request.getRequestURI()
                          .startsWith("/js/")
                || request.getRequestURI()
                          .endsWith(".ico");
    }

}
