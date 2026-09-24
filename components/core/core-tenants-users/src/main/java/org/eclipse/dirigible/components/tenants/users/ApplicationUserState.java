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
import java.util.Optional;

/**
 * An application user as the APIs answer it. Every timestamp is an ISO-8601 string, never a number.
 *
 * @param id the id
 * @param tenantId the tenant id
 * @param email the email
 * @param roles the roles, in every state
 * @param status the account status
 * @param invitedBy who made the first request
 * @param invitedAt when
 * @param updatedBy who last changed the row
 * @param updatedAt when
 * @param lastSignInAt when the person last entered the tenant
 * @param createdAt when the row was created
 */
public record ApplicationUserState(Long id, String tenantId, String email, List<RoleState> roles, String status, String invitedBy,
        String invitedAt, String updatedBy, String updatedAt, String lastSignInAt, String createdAt) {

    /**
     * One role of the user.
     *
     * @param role the role
     * @param state {@code REQUESTED}, {@code GRANTED} or {@code FAILED}
     * @param requestId the latest request for the role - the granting one once granted
     * @param requestedBy who asked for it
     * @param requestedAt when the latest request was published
     * @param grantedBy who granted it
     * @param grantedAt when
     * @param errorMessage the failure text of the latest failed request
     */
    public record RoleState(String role, String state, String requestId, String requestedBy, String requestedAt, String grantedBy,
            String grantedAt, String errorMessage) {
    }

    /**
     * The given role, if the user has a row for it.
     *
     * @param role the role
     * @return the role
     */
    public Optional<RoleState> role(String role) {
        return roles.stream()
                    .filter(row -> row.role()
                                      .equals(role))
                    .findFirst();
    }

    /**
     * The state of a user.
     *
     * @param user the user
     * @return the state
     */
    static ApplicationUserState of(ApplicationUser user) {
        List<RoleState> roles = user.getRoles()
                                    .stream()
                                    .map(row -> new RoleState(row.getRole(), row.getState()
                                                                                .name(),
                                            row.getRequestId(), row.getRequestedBy(), iso(row.getRequestedAt()), row.getGrantedBy(),
                                            iso(row.getGrantedAt()), row.getErrorMessage()))
                                    .toList();
        return new ApplicationUserState(user.getId(), user.getTenantId(), user.getEmail(), roles, user.getStatus()
                                                                                                      .name(),
                user.getInvitedBy(), iso(user.getInvitedAt()), user.getUpdatedBy(), iso(user.getUpdatedAt()), iso(user.getLastSignInAt()),
                iso(user.getCreatedAt()));
    }

    /**
     * ISO-8601, or null.
     *
     * @param instant the instant
     * @return the text
     */
    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
