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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.groups.TenantGroupsParser;
import org.eclipse.dirigible.components.base.tenant.groups.UserTenantAssignments;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Grants a user the tenant they picked.
 *
 * <p>
 * The selection is kept in the HTTP session, where the tenant scope of every following request
 * reads it, and the authorities of the session become the user's global roles plus the roles their
 * groups grant them <em>in that tenant</em>. Selecting again is how a user switches tenant: the
 * session attribute and the authorities are replaced together, with no re-login.
 *
 * <p>
 * The identity provider stays the authority on membership - a selection is only accepted when the
 * user's own groups grant the tenant.
 */
@Component
public class TenantSelectionManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSelectionManager.class);

    private final TenantGroupsClaim groupsClaim;

    private final TenantService tenantService;

    private final SecurityContextRepository securityContextRepository;

    /**
     * Instantiates a new tenant selection manager.
     *
     * @param groupsClaim the claim the user groups are read from
     * @param tenantService the tenant registry of this instance
     */
    public TenantSelectionManager(TenantGroupsClaim groupsClaim, TenantService tenantService) {
        this(groupsClaim, tenantService, new HttpSessionSecurityContextRepository());
    }

    /**
     * Instantiates a new tenant selection manager.
     *
     * @param groupsClaim the claim the user groups are read from
     * @param tenantService the tenant registry of this instance
     * @param securityContextRepository where the rebuilt authentication is persisted
     */
    TenantSelectionManager(TenantGroupsClaim groupsClaim, TenantService tenantService,
            SecurityContextRepository securityContextRepository) {
        this.groupsClaim = groupsClaim;
        this.tenantService = tenantService;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * What the groups of the authenticated user say about this application.
     *
     * @param authentication the authentication; may be {@code null}
     * @return the assignments, never {@code null}
     */
    public UserTenantAssignments assignmentsOf(Authentication authentication) {
        Set<String> groups = groupsClaim.groupsOf(authentication);
        if (groups.isEmpty()) {
            return UserTenantAssignments.empty();
        }
        return TenantGroupsParser.parse(groups, DirigibleConfig.APP_ID.getStringValue());
    }

    /**
     * The tenants the user may enter, in the order their groups name them.
     *
     * @param authentication the authentication; may be {@code null}
     * @return the tenants, never {@code null}
     */
    public List<TenantOption> availableTenants(Authentication authentication) {
        List<TenantOption> options = new ArrayList<>();
        for (String tenantId : assignmentsOf(authentication).tenantIds()) {
            Optional<Tenant> tenant = tenantService.findById(tenantId);
            String name = tenant.map(Tenant::getName)
                                .orElse(tenantId);
            options.add(new TenantOption(tenantId, name, isProvisioned(tenant)));
        }
        return options;
    }

    /**
     * The tenant the session has selected, if any.
     *
     * @param request the request
     * @return the selected tenant id, or {@code null}
     */
    public String selectedTenantId(HttpServletRequest request) {
        Object selected = request.getSession(false) == null ? null
                : request.getSession(false)
                         .getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE);
        return selected == null ? null : selected.toString();
    }

    /**
     * Enters a tenant: stores the selection in the session and rebuilds the authorities of the
     * authenticated user as their global roles plus their roles in that tenant.
     *
     * <p>
     * The tenant scope of the current request was opened before this ran, so the selection takes effect
     * from the next request on - which is why the picker navigates away after a successful selection.
     *
     * @param request the request
     * @param response the response
     * @param tenantId the tenant to enter
     * @return the role names the user now has
     * @throws TenantSelectionException if the user's groups do not grant the tenant, if this instance
     *         has not provisioned it, or if the request carries no interactive session
     */
    public Set<String> selectTenant(HttpServletRequest request, HttpServletResponse response, String tenantId) {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            throw new TenantSelectionException(TenantSelectionException.Reason.NOT_AN_INTERACTIVE_SESSION, tenantId,
                    "Only a logged in user can select a tenant");
        }
        UserTenantAssignments assignments = assignmentsOf(authentication);
        Set<String> tenantRoles = assignments.rolesFor(tenantId);
        if (tenantRoles.isEmpty()) {
            throw new TenantSelectionException(TenantSelectionException.Reason.NOT_A_MEMBER, tenantId,
                    "User [" + authentication.getName() + "] is not assigned to tenant [" + tenantId + "] of this application");
        }
        if (!isProvisioned(tenantService.findById(tenantId))) {
            throw new TenantSelectionException(TenantSelectionException.Reason.NOT_PROVISIONED_HERE, tenantId,
                    "Tenant [" + tenantId + "] is not provisioned in this application yet");
        }
        request.getSession()
               .setAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE, tenantId);
        Set<String> roles = rolesOf(assignments, tenantId);
        reauthenticate(oauth2Authentication, roles, request, response);

        LOGGER.info("User [{}] selected tenant [{}] and has roles [{}].", authentication.getName(), tenantId, roles);
        return roles;
    }

    /**
     * Re-applies the authorities the current selection implies, when they drifted from what the user's
     * groups now grant.
     *
     * <p>
     * Two things make them drift: an access-token refresh rebuilds the authorities from the identity
     * provider, which yields the global roles only, and a group revoked at the identity provider is
     * reflected in the very next token. A selection that is no longer granted is dropped, so the user
     * is asked to pick again instead of keeping roles they lost.
     *
     * @param request the request
     * @param response the response
     */
    public void ensureConsistent(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            return;
        }
        String selectedTenantId = selectedTenantId(request);
        if (selectedTenantId == null) {
            return;
        }
        UserTenantAssignments assignments = assignmentsOf(authentication);
        if (assignments.rolesFor(selectedTenantId)
                       .isEmpty()) {
            LOGGER.info("User [{}] is no longer assigned to the selected tenant [{}]. Dropping the selection.", authentication.getName(),
                    selectedTenantId);
            request.getSession()
                   .removeAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE);
            reauthenticate(oauth2Authentication, assignments.globalRoles(), request, response);
            return;
        }
        Set<String> expectedRoles = rolesOf(assignments, selectedTenantId);
        Set<String> currentRoles = new LinkedHashSet<>(AuthoritiesUtil.toRoleNames(authentication.getAuthorities()));
        if (!currentRoles.equals(expectedRoles)) {
            LOGGER.debug("Re-applying the roles of tenant [{}] for user [{}]: [{}] instead of [{}].", selectedTenantId,
                    authentication.getName(), expectedRoles, currentRoles);
            reauthenticate(oauth2Authentication, expectedRoles, request, response);
        }
    }

    private Set<String> rolesOf(UserTenantAssignments assignments, String tenantId) {
        Set<String> roles = new LinkedHashSet<>(assignments.globalRoles());
        roles.addAll(assignments.rolesFor(tenantId));
        return roles;
    }

    /**
     * Replaces the authentication of the session with one carrying the given roles, the way the session
     * revalidation does after a token refresh. The session id is deliberately kept: it is the same user
     * in the same identity provider session.
     *
     * @param authentication the current authentication
     * @param roles the role names to grant
     * @param request the request
     * @param response the response
     */
    private void reauthenticate(OAuth2AuthenticationToken authentication, Set<String> roles, HttpServletRequest request,
            HttpServletResponse response) {
        Collection<GrantedAuthority> authorities = new LinkedHashSet<>(AuthoritiesUtil.toAuthorities(roles));
        OAuth2AuthenticationToken reauthenticated = new OAuth2AuthenticationToken(authentication.getPrincipal(), authorities,
                authentication.getAuthorizedClientRegistrationId());
        reauthenticated.setDetails(authentication.getDetails());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(reauthenticated);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    private boolean isProvisioned(Optional<Tenant> tenant) {
        return tenant.filter(found -> TenantStatus.PROVISIONED == found.getStatus())
                     .isPresent();
    }
}
