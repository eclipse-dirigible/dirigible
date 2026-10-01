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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One user of the replica as it is read back - by the provisioning system's diagnostics and by the
 * Owner's list. Timestamps are ISO-8601 strings; {@code lastSignInAt} is this application's own.
 *
 * @param id the id the Owner's commands address the user by
 * @param email the email
 * @param status the status
 * @param revision the revision - the token a change command sends back as its expected revision
 * @param roles the roles held, being added or being removed, by name
 * @param invitedBy who asked for the invitation that began this membership
 * @param invitedAt when
 * @param lastChangedBy who last changed the membership
 * @param lastChangedAt when
 * @param lastError why the latest change was not applied, or null
 * @param lastSignInAt when the person last entered the tenant, or null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TenantUserView(Long id, String email, String status, long revision, List<TenantUserSync.RoleState> roles, String invitedBy,
        String invitedAt, String lastChangedBy, String lastChangedAt, TenantUserSync.LastError lastError, String lastSignInAt) {

    /**
     * The view of a row. Reads the roles, so it is called inside the transaction that loaded it.
     *
     * @param user the row
     * @return the view
     */
    static TenantUserView of(TenantUser user) {
        List<TenantUserSync.RoleState> roles = user.getRoles()
                                                   .stream()
                                                   .map(role -> new TenantUserSync.RoleState(role.getRole(), role.getState()
                                                                                                                 .name(),
                                                           role.getGrantedBy(), text(role.getGrantedAt())))
                                                   .toList();
        TenantUserSync.LastError lastError =
                user.getLastErrorCode() == null ? null : new TenantUserSync.LastError(user.getLastErrorCode(), user.getLastErrorMessage());
        return new TenantUserView(user.getId(), user.getEmail(), user.getStatus()
                                                                     .name(),
                user.getRevision(), roles, user.getInvitedBy(), text(user.getInvitedAt()), user.getLastChangedBy(),
                text(user.getLastChangedAt()), lastError, text(user.getLastSignInAt()));
    }

    private static String text(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
