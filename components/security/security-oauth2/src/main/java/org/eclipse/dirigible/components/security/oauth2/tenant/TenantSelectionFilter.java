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

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.eclipse.dirigible.components.base.tenant.groups.UserTenantAssignments;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSupport;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.TokenKind;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantSelectionEndpoint.TenantSelectionRefusal;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Makes sure a request knows which tenant it is in.
 *
 * <p>
 * An interactive session keeps its choice: a user of exactly one tenant is put into it without
 * being asked. A user of several is sent to the picker - a browser by redirect, anything
 * programmatic by a {@code 409} naming the choices, so an API client is told what to do rather than
 * silently landing in the wrong tenant. A user of none is let through if they have global roles
 * (staff of the instance) and refused otherwise.
 *
 * <p>
 * A bearer request has no session to keep a choice in, so it names its tenant on every request, in
 * the {@link TenantSelectionConstants#TENANT_HEADER} header. The tenant is validated against the
 * token's own groups exactly as a session selection is, the roles the groups grant in it are added
 * for this request only, and the rest of the chain runs in that tenant. Without the header the
 * request stays in the default tenant with the token's global roles - and an ID token whose groups
 * grant no global role is refused, as the session of such a user is: it has nothing to do there. An
 * access token carries no groups and passes as it always did.
 *
 * <p>
 * It runs <em>before</em> authorization on purpose: until a tenant is entered the user has no
 * tenant roles, so authorization would answer 403 before they ever saw the picker or the refusal.
 *
 * <p>
 * Requests that carry neither an interactive session nor a bearer token - anonymous requests, basic
 * authentication - pass through untouched; their tenant is the default one.
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
     * selection endpoint, authentication, error pages and the platform's own status surfaces. The
     * prefixes match by {@code startsWith}, so the selection endpoint's entry also covers
     * {@link CurrentTenantEndpoint} beneath it - deliberately: a shell asking where it is must be
     * answered, not sent to choose.
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

    private final ObjectProvider<ResourceServerJwtSupport> bearerTokenSupport;

    private final TenantResolutionStrategy resolutionStrategy;

    private final Gson gson;

    /**
     * Instantiates a new tenant selection filter.
     *
     * @param tenantSelectionManager the tenant selection manager
     * @param tenantContext the tenant scope of the current execution
     * @param bearerTokenSupport the bearer token rules of the login profile, present on the profiles
     *        that accept bearer tokens - it tells an ID token, which identifies a user, from an access
     *        token
     */
    public TenantSelectionFilter(TenantSelectionManager tenantSelectionManager, TenantContext tenantContext,
            ObjectProvider<ResourceServerJwtSupport> bearerTokenSupport) {
        this.tenantSelectionManager = tenantSelectionManager;
        this.tenantContext = tenantContext;
        this.bearerTokenSupport = bearerTokenSupport;
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
        if (authentication instanceof JwtAuthenticationToken bearer) {
            serveBearer(bearer, request, response, chain);
            return;
        }
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
     * Serves a bearer request in the tenant it names, or in the default tenant when it names none.
     *
     * @param bearer the authenticated bearer token
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void serveBearer(JwtAuthenticationToken bearer, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = requestedTenant(request);
        if (tenantId == null) {
            if (isUserToken(bearer) && tenantSelectionManager.assignmentsOf(bearer)
                                                             .globalRoles()
                                                             .isEmpty()) {
                LOGGER.warn("Bearer user [{}] holds no global role of this application and names no tenant in [{}].", bearer.getName(),
                        TenantSelectionConstants.TENANT_HEADER);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User holds no global role of this application and the request"
                        + " names no tenant in the [" + TenantSelectionConstants.TENANT_HEADER + "] header");
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        BearerTenantSelection selection;
        try {
            selection = tenantSelectionManager.enterTenant(bearer, tenantId);
        } catch (TenantSelectionException ex) {
            refuse(response, ex);
            return;
        }
        // The tenant scope of this request was opened as the default tenant's before the token was
        // authenticated (see TenantExtractor), and the authorities are the token's global roles - both
        // are replaced for the rest of the chain, which is all a bearer request lives for.
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(selection.authentication());
        SecurityContextHolder.setContext(securityContext);
        continueInTenant(selection.tenant(), request, response, chain);
    }

    /**
     * The tenant a bearer request names, or {@code null} for none - a blank header counts as none.
     */
    private static String requestedTenant(HttpServletRequest request) {
        String header = request.getHeader(TenantSelectionConstants.TENANT_HEADER);
        if (header == null) {
            return null;
        }
        String tenantId = header.trim();
        return tenantId.isEmpty() ? null : tenantId;
    }

    /**
     * Whether a bearer token identifies a user - an ID token - as opposed to the access token of a
     * machine client, which carries no groups and is not held to having a tenant.
     */
    private boolean isUserToken(JwtAuthenticationToken bearer) {
        ResourceServerJwtSupport support = bearerTokenSupport.getIfAvailable();
        return support != null && support.settings()
                                         .kindOf(bearer.getToken())
                                         .filter(TokenKind.ID::equals)
                                         .isPresent();
    }

    /**
     * Answers a bearer request whose tenant cannot be entered, with the status and the body the
     * selection endpoint answers a session's refused selection with.
     *
     * @param response the response
     * @param refusal the refusal
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void refuse(HttpServletResponse response, TenantSelectionException refusal) throws IOException {
        LOGGER.info("Refused the tenant [{}] a bearer request named: {}", refusal.getTenantId(), refusal.getMessage());
        response.setStatus(refusal.getReason()
                                  .httpStatus()
                                  .value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(gson.toJson(TenantSelectionRefusal.of(refusal)));
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
            // A tenant this instance does not know is not a wait-and-retry: the user is left on a picker
            // holding one tile they can never press, and only an operator can resolve it.
            if (TenantSelectionException.Reason.UNKNOWN_HERE == ex.getReason()) {
                LOGGER.warn("The only tenant [{}] of user [{}] cannot be entered: {}", tenantId, authentication.getName(), ex.getMessage());
            } else {
                LOGGER.info("The only tenant [{}] of user [{}] cannot be entered: {}", tenantId, authentication.getName(), ex.getMessage());
            }
            return false;
        }
    }

    /**
     * Runs the rest of the chain in the scope of a tenant this instance knows by id.
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
        runScoped(() -> tenantContext.execute(tenantId, () -> {
            chain.doFilter(request, response);
            return null;
        }));
    }

    /**
     * Runs the rest of the chain in the scope of a tenant already resolved.
     *
     * @param tenant the tenant to run in
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void continueInTenant(Tenant tenant, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        runScoped(() -> tenantContext.execute(tenant, () -> {
            chain.doFilter(request, response);
            return null;
        }));
    }

    /**
     * Runs a scoped continuation of the chain, letting the chain's own exceptions through unchanged.
     */
    private static void runScoped(CallableResultAndException<Void, Exception> scoped) throws ServletException, IOException {
        try {
            scoped.call();
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
