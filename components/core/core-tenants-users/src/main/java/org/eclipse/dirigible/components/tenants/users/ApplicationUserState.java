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

/**
 * An application user as the APIs answer it. Every timestamp is an ISO-8601 string, never a number.
 *
 * @param id the id
 * @param tenantId the tenant id
 * @param email the email
 * @param roles the granted roles
 * @param status the account status
 * @param errorMessage the failure text of the latest failed request
 * @param requestId the latest request's message id
 * @param requestedRole the role the latest request asked for
 * @param requestState the latest request's state
 * @param requestedAt when the latest request was published
 * @param invitedBy who made the first request
 * @param invitedAt when
 * @param updatedBy who last changed the row
 * @param updatedAt when
 * @param lastSignInAt when the person last entered the tenant
 * @param createdAt when the row was created
 */
public record ApplicationUserState(Long id, String tenantId, String email, List<GrantedRole> roles, String status, String errorMessage,
        String requestId, String requestedRole, String requestState, String requestedAt, String invitedBy, String invitedAt,
        String updatedBy, String updatedAt, String lastSignInAt, String createdAt) {

    /**
     * One granted role.
     *
     * @param role the role
     * @param grantedBy who granted it
     * @param grantedAt when
     * @param requestId the granting request
     */
    public record GrantedRole(String role, String grantedBy, String grantedAt, String requestId) {
    }

    /**
     * The state of a user.
     *
     * @param user the user
     * @return the state
     */
    static ApplicationUserState of(ApplicationUser user) {
        List<GrantedRole> roles = user.getRoles()
                                      .stream()
                                      .map(role -> new GrantedRole(role.getRole(), role.getGrantedBy(), iso(role.getGrantedAt()),
                                              role.getRequestId()))
                                      .toList();
        return new ApplicationUserState(user.getId(), user.getTenantId(), user.getEmail(), roles, user.getStatus()
                                                                                                      .name(),
                user.getErrorMessage(), user.getRequestId(), user.getRequestedRole(),
                user.getRequestState() == null ? null
                        : user.getRequestState()
                              .name(),
                iso(user.getRequestedAt()), user.getInvitedBy(), iso(user.getInvitedAt()), user.getUpdatedBy(), iso(user.getUpdatedAt()),
                iso(user.getLastSignInAt()), iso(user.getCreatedAt()));
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
