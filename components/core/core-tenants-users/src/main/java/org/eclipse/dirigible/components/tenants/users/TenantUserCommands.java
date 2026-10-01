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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * The tenant Owner's three commands - invite a person with roles, set a member's roles, remove a
 * member - each one published change request.
 *
 * <p>
 * The input shape is checked here for real. The business rules - already a member, a change in
 * flight, a stale screen, no change, the last owner - are checked against the replica only to
 * answer the Owner at once: the provisioning system decides them again, so a check passed here is
 * no promise. Nothing is written here; the replica changes when the provisioning system reports
 * what it did.
 */
@Service
@Conditional(TenantUsersEnabledCondition.class)
class TenantUserCommands {

    /** The replica. */
    private final TenantUserReplicaService replica;

    /** The publisher. */
    private final TenantUserChangePublisher publisher;

    /** The access rules. */
    private final TenantUsersAccess access;

    /**
     * Instantiates the commands.
     *
     * @param replica the replica
     * @param publisher the publisher
     * @param access the access rules
     */
    TenantUserCommands(TenantUserReplicaService replica, TenantUserChangePublisher publisher, TenantUsersAccess access) {
        this.replica = replica;
        this.publisher = publisher;
        this.access = access;
    }

    /**
     * Invites a person into the tenant with one or more roles.
     *
     * @param tenantId the session's tenant
     * @param body the person and roles
     * @return the accepted request
     */
    Accepted invite(String tenantId, Invite body) {
        String email = requireEmail(body == null ? null : body.email());
        List<String> roles = requireRoles(body == null ? null : body.roles());
        replica.find(tenantId, email)
               .ifPresent(user -> {
                   if (isMember(user)) {
                       throw conflict("ALREADY_MEMBER", "[" + email + "] is already a member of this tenant - change their roles instead");
                   }
                   if (inProgress(user)) {
                       throw conflict("REQUEST_PENDING", "A change for [" + email + "] is still in progress");
                   }
               });
        return publish(tenantId, TenantUserChangeRequest.Action.INVITE, email, roles, null);
    }

    /**
     * Sets a member's roles - adding and removing in one change.
     *
     * @param tenantId the session's tenant
     * @param id the user id
     * @param body the desired roles and the revision the screen showed
     * @return the accepted request
     */
    Accepted setRoles(String tenantId, long id, RolesChange body) {
        List<String> roles = requireRoles(body == null ? null : body.roles());
        long expectedRevision = requireRevision(body == null ? null : body.expectedRevision());
        TenantUserView user = requireUser(tenantId, id);
        if (inProgress(user)) {
            throw conflict("REQUEST_PENDING", "A change for [" + user.email() + "] is still in progress");
        }
        if (!isMember(user)) {
            throw conflict("NOT_A_MEMBER", "[" + user.email() + "] is not a member whose roles can be changed");
        }
        requireRevisionMatches(user, expectedRevision);
        if (granted(user).equals(Set.copyOf(roles))) {
            throw conflict("NO_CHANGE", "[" + user.email() + "] already holds exactly these roles");
        }
        if (!roles.contains(ApplicationRoles.OWNER)) {
            requireAnotherOwner(tenantId, user);
        }
        return publish(tenantId, TenantUserChangeRequest.Action.UPDATE_ROLES, user.email(), roles, expectedRevision);
    }

    /**
     * Removes a member from the tenant - or dismisses a failed invitation.
     *
     * @param tenantId the session's tenant
     * @param id the user id
     * @param body the revision the screen showed
     * @return the accepted request
     */
    Accepted remove(String tenantId, long id, Removal body) {
        long expectedRevision = requireRevision(body == null ? null : body.expectedRevision());
        TenantUserView user = requireUser(tenantId, id);
        // Removing a failed row is the Dismiss: there is nothing in flight and nothing held to protect.
        if (!TenantUserStatus.FAILED.name()
                                    .equals(user.status())) {
            if (inProgress(user)) {
                throw conflict("REQUEST_PENDING", "A change for [" + user.email() + "] is still in progress");
            }
            requireRevisionMatches(user, expectedRevision);
            requireAnotherOwner(tenantId, user);
        }
        return publish(tenantId, TenantUserChangeRequest.Action.REMOVE, user.email(), null, expectedRevision);
    }

    private Accepted publish(String tenantId, TenantUserChangeRequest.Action action, String email, List<String> roles,
            Long expectedRevision) {
        String requestId = UUID.randomUUID()
                               .toString();
        publisher.publish(
                new TenantUserChangeRequest(requestId, TenantUserChangeRequest.TYPE, TenantUserChangeRequest.VERSION, action, tenantId,
                        DirigibleConfig.APP_ID.getStringValue(), email, roles, expectedRevision, access.requestedBy(), Instant.now()
                                                                                                                              .toString()));
        return new Accepted(requestId);
    }

    private static String requireEmail(String raw) {
        String email = raw == null ? "" : TenantUserRules.normalize(raw);
        if (!TenantUserRules.isEmail(email)) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL",
                    email.isEmpty() ? "The request names no email address" : "[" + email + "] is not an email address");
        }
        return email;
    }

    /** The desired roles, trimmed, sorted and without duplicates - each one an application role. */
    private static List<String> requireRoles(List<String> raw) {
        Set<String> roles = new TreeSet<>();
        for (String role : raw == null ? List.<String>of() : raw) {
            String trimmed = role == null ? "" : role.trim();
            if (!ApplicationRoles.ALL.contains(trimmed)) {
                throw new TenantUsersException(HttpStatus.BAD_REQUEST, "INVALID_ROLE",
                        "[" + trimmed + "] is not a role of this application - one of " + ApplicationRoles.ALL);
            }
            roles.add(trimmed);
        }
        if (roles.isEmpty()) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "NO_ROLES", "Name at least one role - one of " + ApplicationRoles.ALL);
        }
        return List.copyOf(roles);
    }

    private static long requireRevision(Long expectedRevision) {
        if (expectedRevision == null) {
            throw new TenantUsersException(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                    "The request names no expectedRevision - the revision of the user as the screen showed it");
        }
        return expectedRevision;
    }

    private TenantUserView requireUser(String tenantId, long id) {
        return replica.find(tenantId, id)
                      .filter(user -> !TenantUserStatus.REMOVED.name()
                                                               .equals(user.status()))
                      .orElseThrow(() -> new TenantUsersException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                              "There is no user [" + id + "] in this tenant"));
    }

    private static void requireRevisionMatches(TenantUserView user, long expectedRevision) {
        if (user.revision() != expectedRevision) {
            throw conflict("STALE_REVISION", "[" + user.email() + "] changed since the screen was loaded - reload and try again");
        }
    }

    /** Refuses to take the Owner role away from the only member who holds it. */
    private void requireAnotherOwner(String tenantId, TenantUserView user) {
        if (!granted(user).contains(ApplicationRoles.OWNER)) {
            return;
        }
        boolean another = replica.list(tenantId, false)
                                 .stream()
                                 .anyMatch(other -> !Objects.equals(other.id(), user.id())
                                         && granted(other).contains(ApplicationRoles.OWNER));
        if (!another) {
            throw conflict("LAST_OWNER", "[" + user.email() + "] is the only owner of this tenant - make someone else an owner first");
        }
    }

    private static boolean isMember(TenantUserView user) {
        return TenantUserStatus.INVITED.name()
                                       .equals(user.status())
                || TenantUserStatus.ASSIGNED.name()
                                            .equals(user.status());
    }

    /** A change is in progress while the user is pending or any role is being added or removed. */
    private static boolean inProgress(TenantUserView user) {
        return TenantUserStatus.PENDING.name()
                                       .equals(user.status())
                || user.roles()
                       .stream()
                       .anyMatch(role -> !TenantUserRoleState.GRANTED.name()
                                                                     .equals(role.state()));
    }

    private static Set<String> granted(TenantUserView user) {
        return user.roles()
                   .stream()
                   .filter(role -> TenantUserRoleState.GRANTED.name()
                                                              .equals(role.state()))
                   .map(TenantUserSync.RoleState::role)
                   .collect(Collectors.toSet());
    }

    private static TenantUsersException conflict(String reason, String message) {
        return new TenantUsersException(HttpStatus.CONFLICT, reason, message);
    }

    /**
     * An invitation.
     *
     * @param email the person
     * @param roles the roles to give them
     */
    record Invite(String email, List<String> roles) {
    }

    /**
     * A change of a member's roles.
     *
     * @param roles the roles they should hold
     * @param expectedRevision their revision as the screen showed it
     */
    record RolesChange(List<String> roles, Long expectedRevision) {
    }

    /**
     * A removal.
     *
     * @param expectedRevision the user's revision as the screen showed it
     */
    record Removal(Long expectedRevision) {
    }

    /**
     * An accepted command.
     *
     * @param requestId the id of the published request - for logs and support
     */
    record Accepted(String requestId) {
    }
}
