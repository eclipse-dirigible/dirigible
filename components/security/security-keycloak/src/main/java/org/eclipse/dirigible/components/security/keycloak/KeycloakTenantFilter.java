/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.tenant.TenantExtractor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Class KeycloakTenantFilter.
 */
@Profile("keycloak")
@Component
public class KeycloakTenantFilter extends OncePerRequestFilter {

    /** The tenant service. */
    private final TenantExtractor tenantExtractor;
    private final boolean multitenantModeEnabled;
    private final boolean multitenantModeKeycloakSingleRealm;

    /**
     * Instantiates a new tenant context init filter.
     *
     * @param tenantExtractor the tenant extractor
     */
    public KeycloakTenantFilter(TenantExtractor tenantExtractor) {
        this.tenantExtractor = tenantExtractor;
        this.multitenantModeEnabled = DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue();
        this.multitenantModeKeycloakSingleRealm = DirigibleConfig.MULTI_TENANT_MODE_KEYCLOAK_SINGLE_REALM_ENABLED.getBooleanValue();
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
        if (multitenantModeEnabled && multitenantModeKeycloakSingleRealm) {
            Optional<Tenant> currentTenant = tenantExtractor.determineTenantSubdomain(request);
            if (currentTenant.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "There is no registered tenant for the current host");
                return;
            }

            Principal principal = request.getUserPrincipal();
            if (principal instanceof OAuth2AuthenticationToken oauthToken) {
                String tenantAttribute = oauthToken.getPrincipal()
                                                   .getAttribute("custom:tenant");
                if (tenantAttribute == null || tenantAttribute.equals("")) {
                    forbidden("User is not assigned to any tenant", response);
                    return;
                }
                Set<String> userTenants = new HashSet<>(Arrays.asList(tenantAttribute.split(","))
                                                              .stream()
                                                              .map(e -> e.trim())
                                                              .collect(Collectors.toList()));
                if (!userTenants.contains(currentTenant.get()
                                                       .getSubdomain())) {
                    forbidden("User is not member of the [" + currentTenant.get()
                                                                           .getName()
                            + " | " + currentTenant.get()
                                                   .getSubdomain()
                            + "] tenant", response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
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
                          .endsWith(".ico")
                || request.getRequestURI()
                          .startsWith("/index-busy.html")
                || request.getRequestURI()
                          .startsWith("/services/js/platform-core/extension-services/themes.js")
                || request.getRequestURI()
                          .startsWith("/services/js/platform-core/services/loader.js");
    }

    /**
     * Forbidden.
     *
     * @param message the message
     * @param response the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void forbidden(String message, HttpServletResponse response) throws IOException {
        logger.warn(message);
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }
}
