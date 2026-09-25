/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.eclipse.dirigible.components.base.http.roles.Roles.RoleNames;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The users of the current tenant, for its owners: who they are, where each stands, and an
 * invitation for another person.
 *
 * <p>
 * Protected like every other service, with {@code @RolesAllowed}: managing is for the
 * {@link ApplicationRoles#OWNER} of the selected tenant, a developer or an administrator; reading
 * also for an operator; the context answers every authenticated user so a page can decide whether
 * to offer the section. The tenant is always the caller's selected tenant - never a request field -
 * and never the default one. Both POSTs accept JSON only, which is what keeps a cross-site form
 * from triggering them (the chains disable CSRF tokens; the tenant selection endpoint relies on the
 * same).
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenant-users")
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersEndpoint extends BaseEndpoint {

    /** The access rules. */
    private final TenantUsersAccess access;

    /** The users. */
    private final ApplicationUserService users;

    /** The invitations. */
    private final TenantUserInvitationService invitations;

    /**
     * Instantiates the endpoint.
     *
     * @param access the access rules
     * @param users the users
     * @param invitations the invitations
     */
    TenantUsersEndpoint(TenantUsersAccess access, ApplicationUserService users, TenantUserInvitationService invitations) {
        this.access = access;
        this.users = users;
        this.invitations = invitations;
    }

    /**
     * Whether the caller may see and manage users here - answers every authenticated user.
     *
     * @return the context
     */
    @GetMapping(path = "/context", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantUsersContext> context() {
        boolean canManage = access.canManage();
        boolean canRead = access.canRead();
        String tenantId = canRead ? access.requireTenant() : null;
        return ResponseEntity.ok(new TenantUsersContext(true, tenantId, canManage, canRead, ApplicationRoles.OWNER, ApplicationRoles.ALL));
    }

    /**
     * The users of the current tenant.
     *
     * @return the users
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR, RoleNames.OPERATOR})
    ResponseEntity<List<ApplicationUserState>> list() {
        return ResponseEntity.ok(users.listOf(access.requireTenant()));
    }

    /**
     * Invites a person into the current tenant.
     *
     * @param request the person and role
     * @return 202 with the user
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
    ResponseEntity<ApplicationUserState> invite(@RequestBody InvitationRequest request) {
        String tenantId = access.requireTenant();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(invitations.invite(tenantId, request, access.callerName()));
    }

    /**
     * Publishes a user's unanswered request for a role again, with the same id.
     *
     * @param id the user id
     * @param role the role
     * @return 202 with the user
     */
    @PostMapping(path = "/{id}/roles/{role}/resend", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
    ResponseEntity<ApplicationUserState> resend(@PathVariable("id") long id, @PathVariable("role") String role) {
        String tenantId = access.requireTenant();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(invitations.resend(tenantId, id, role, access.callerName()));
    }
}
