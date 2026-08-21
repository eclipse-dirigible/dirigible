/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.eclipse.dirigible.components.base.tenant.groups.UserTenantAssignments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Makes sure an interactive request knows which tenant it is in.
 *
 * <p>
 * A user of exactly one tenant is put into it without being asked. A user of several is sent to the
 * picker - a browser by redirect, anything programmatic by a {@code 409} naming the choices, so an
 * API client is told what to do rather than silently landing in the wrong tenant. A user of none is
 * let through if they have global roles (staff of the instance) and refused otherwise.
 *
 * <p>
 * It runs <em>before</em> authorization on purpose: until a tenant is selected the user has no
 * tenant roles, so authorization would answer 403 before they ever saw the picker.
 *
 * <p>
 * Requests that carry no interactive session - machine-to-machine bearer tokens, anonymous
 * requests, basic authentication - pass through untouched; their tenant is the default one.
 */
@Component
public class TenantSelectionFilter extends OncePerRequestFilter {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSelectionFilter.class);

    /** The page a browser is sent to when a choice has to be made. */
    public static final String TENANT_SELECTION_PAGE = "/tenant-selection.html";

    /** Answered to a programmatic caller that has to choose. */
    private static final String TENANT_SELECTION_REQUIRED = "TENANT_SELECTION_REQUIRED";

    /**
     * What a request may need before a tenant is known: the picker itself and what it loads, the
     * selection endpoint, authentication, error pages and the platform's own status surfaces.
     */
    private static final List<String> UNFILTERED_PREFIXES = List.of( //
            TENANT_SELECTION_PAGE, //
            "/services/security/tenant-selection", //
            "/webjars/", //
            "/services/web/platform-core/", //
            "/services/js/platform-core/", //
            "/services/js/platform-branding/", //
            "/services/core/theme/", //
            "/services/core/healthcheck", //
            "/services/core/readiness", //
            "/login", //
            "/logout", //
            "/oauth2/", //
            "/error", //
            "/actuator/", //
            "/index-busy.html");

    private final TenantSelectionManager tenantSelectionManager;

    private final TenantContext tenantContext;

    private final TenantResolutionStrategy resolutionStrategy;

    private final Gson gson;

    /**
     * Instantiates a new tenant selection filter.
     *
     * @param tenantSelectionManager the tenant selection manager
     * @param tenantContext the tenant scope of the current execution
     */
    public TenantSelectionFilter(TenantSelectionManager tenantSelectionManager, TenantContext tenantContext) {
        this.tenantSelectionManager = tenantSelectionManager;
        this.tenantContext = tenantContext;
        this.resolutionStrategy = TenantResolutionStrategy.fromConfiguration();
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
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            chain.doFilter(request, response);
            return;
        }
        if (tenantSelectionManager.selectedTenantId(request) != null) {
            tenantSelectionManager.ensureConsistent(request, response);
            chain.doFilter(request, response);
            return;
        }
        UserTenantAssignments assignments = tenantSelectionManager.assignmentsOf(authentication);
        if (assignments.hasNoTenants()) {
            if (assignments.globalRoles()
                           .isEmpty()) {
                LOGGER.warn("User [{}] is not assigned to any tenant of this application.", authentication.getName());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not assigned to any tenant of this application");
                return;
            }
            LOGGER.debug("User [{}] is assigned to no tenant but has global roles. Passing through.", authentication.getName());
            chain.doFilter(request, response);
            return;
        }
        if (assignments.tenantIds()
                       .size() == 1) {
            String onlyTenantId = assignments.tenantIds()
                                             .iterator()
                                             .next();
            if (autoSelect(request, response, onlyTenantId, authentication)) {
                // The tenant scope of this request was opened before the selection existed, so it is
                // still the default tenant's. Continuing in it would serve the request with the roles
                // of the selected tenant and the data of another one - so the rest of the chain runs
                // in the tenant just entered.
                continueInTenant(onlyTenantId, request, response, chain);
                return;
            }
        }
        requireSelection(request, response, authentication);
    }

    /**
     * A user of a single tenant is not asked which one.
     *
     * @param request the request
     * @param response the response
     * @param tenantId the only tenant of the user
     * @param authentication the authenticated user
     * @return true if the tenant was entered
     */
    private boolean autoSelect(HttpServletRequest request, HttpServletResponse response, String tenantId, Authentication authentication) {
        try {
            tenantSelectionManager.selectTenant(request, response, tenantId);
            return true;
        } catch (TenantSelectionException ex) {
            LOGGER.info("The only tenant [{}] of user [{}] cannot be entered: {}", tenantId, authentication.getName(), ex.getMessage());
            return false;
        }
    }

    /**
     * Runs the rest of the chain in the scope of a tenant.
     *
     * @param tenantId the tenant to run in
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void continueInTenant(String tenantId, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            tenantContext.execute(tenantId, () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    /**
     * Sends the user to the picker: a browser by redirect, a programmatic caller by a conflict naming
     * the choices.
     *
     * @param request the request
     * @param response the response
     * @param authentication the authenticated user
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void requireSelection(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (prefersHtml(request)) {
            LOGGER.debug("User [{}] has to select a tenant. Redirecting to the picker.", authentication.getName());
            response.sendRedirect(request.getContextPath() + TENANT_SELECTION_PAGE);
            return;
        }
        LOGGER.debug("User [{}] has to select a tenant. Answering the programmatic caller with a conflict.", authentication.getName());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", TENANT_SELECTION_REQUIRED);
        body.put("tenants", tenantSelectionManager.availableTenants(authentication));
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(gson.toJson(body));
    }

    private boolean prefersHtml(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null) {
            return false;
        }
        String lowerCaseAccept = accept.toLowerCase();
        if (lowerCaseAccept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return false;
        }
        return lowerCaseAccept.contains(MediaType.TEXT_HTML_VALUE);
    }

    /**
     * Should not filter.
     *
     * @param request the request
     * @return true, if the request must work before a tenant is known
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (TenantResolutionStrategy.TOKEN_GROUPS != resolutionStrategy) {
            return true;
        }
        String path = request.getRequestURI();
        return UNFILTERED_PREFIXES.stream()
                                  .anyMatch(path::startsWith);
    }
}
