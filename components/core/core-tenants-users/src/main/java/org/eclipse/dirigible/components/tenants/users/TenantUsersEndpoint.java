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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The users of the current tenant, for its owners: who they are and where each stands, and the
 * three commands - invite a person with roles, set a member's roles, remove a member. Each command
 * is one published change request, answered 202 with its id; the list shows its outcome once the
 * provisioning system reports it.
 *
 * <p>
 * Protected like every other service, with {@code @RolesAllowed}: managing is for the
 * {@link ApplicationRoles#OWNER} of the selected tenant, a developer or an administrator; reading
 * also for an operator; the context answers every authenticated user so a page can decide whether
 * to offer the section. The tenant is always the caller's selected tenant - never a request field -
 * and never the default one. Every body is read as JSON only, which is what keeps a cross-site form
 * from triggering a command (the chains disable CSRF tokens; the tenant selection endpoint relies
 * on the same): a form post is refused 415. The bodies declare no {@code consumes} so that refusal
 * is answered here, in the endpoint's own shape.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenant-users")
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersEndpoint extends BaseEndpoint {

    /** The access rules. */
    private final TenantUsersAccess access;

    /** The replica. */
    private final TenantUserReplicaService replica;

    /** The commands. */
    private final TenantUserCommands commands;

    /**
     * Instantiates the endpoint.
     *
     * @param access the access rules
     * @param replica the replica
     * @param commands the commands
     */
    TenantUsersEndpoint(TenantUsersAccess access, TenantUserReplicaService replica, TenantUserCommands commands) {
        this.access = access;
        this.replica = replica;
        this.commands = commands;
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
        return ResponseEntity.ok(new TenantUsersContext(true, tenantId, canManage, canRead, ApplicationRoles.OWNER, ApplicationRoles.ALL,
                access.requestedBy()));
    }

    /**
     * The users of the current tenant, removed ones hidden.
     *
     * @return the users, by email
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR, RoleNames.OPERATOR})
    ResponseEntity<List<TenantUserView>> list() {
        return ResponseEntity.ok(replica.list(access.requireTenant(), false));
    }

    /**
     * Invites a person into the current tenant with one or more roles.
     *
     * @param body the person and roles
     * @return 202 with the request id
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
    ResponseEntity<TenantUserCommands.Accepted> invite(@RequestBody(required = false) TenantUserCommands.Invite body) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(commands.invite(access.requireTenant(), body));
    }

    /**
     * Sets a member's roles - the roles they should hold, adding and removing in one change.
     *
     * @param id the user id
     * @param body the roles and the revision the screen showed
     * @return 202 with the request id
     */
    @PutMapping(path = "/{id}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
    ResponseEntity<TenantUserCommands.Accepted> setRoles(@PathVariable("id") long id,
            @RequestBody(required = false) TenantUserCommands.RolesChange body) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(commands.setRoles(access.requireTenant(), id, body));
    }

    /**
     * Removes a member from the current tenant, or dismisses a failed invitation.
     *
     * @param id the user id
     * @param body the revision the screen showed
     * @return 202 with the request id
     */
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed({ApplicationRoles.OWNER, RoleNames.DEVELOPER, RoleNames.ADMINISTRATOR})
    ResponseEntity<TenantUserCommands.Accepted> remove(@PathVariable("id") long id,
            @RequestBody(required = false) TenantUserCommands.Removal body) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                             .body(commands.remove(access.requireTenant(), id, body));
    }
}
