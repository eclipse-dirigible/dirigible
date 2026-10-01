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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The body of the users snapshot endpoint: the users the provisioning system changed, or with
 * {@code complete: true} the tenant's full list. An absent field is null - the provisioning
 * system's serializer omits nulls.
 *
 * @param complete true for the tenant's full list (a resync), absent or false for the changed users
 * @param revision the tenant's revision sequence as the provisioning system read it - the resync's
 *        watermark
 * @param users the users
 */
record TenantUserSync(Boolean complete, Long revision, List<Snapshot> users) {

    /**
     * Whether the body is the tenant's full list.
     *
     * @return whether it is a resync
     */
    boolean isComplete() {
        return Boolean.TRUE.equals(complete);
    }

    /**
     * One user as the provisioning system last knows them. Timestamps are ISO-8601 strings.
     *
     * @param email the email - the identity key with the tenant
     * @param revision the user's revision: the sequence value at their last change
     * @param status the status
     * @param roles the roles held or being added; a role absent here is not held
     * @param invitedBy who asked for the invitation that began this membership
     * @param invitedAt when
     * @param lastChangedBy who last changed the membership
     * @param lastChangedAt when
     * @param lastError why the latest change was not applied, or null
     */
    record Snapshot(String email, Long revision, String status, List<RoleState> roles, String invitedBy, String invitedAt,
            String lastChangedBy, String lastChangedAt, LastError lastError) {
    }

    /**
     * One role of a user.
     *
     * @param role the role
     * @param state {@code GRANTED}, {@code ADDING} or {@code REMOVING}
     * @param grantedBy who granted it; null while it is being added
     * @param grantedAt when; null while it is being added
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RoleState(String role, String state, String grantedBy, String grantedAt) {
    }

    /**
     * Why the latest change was not applied.
     *
     * @param code a reason code of the change message, or {@code FAILED}
     * @param message the text that goes with it
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LastError(String code, String message) {
    }

    /**
     * What an apply did.
     *
     * @param applied users written
     * @param ignored users whose revision was not higher than the stored one - normal, never an error
     * @param removed rows a resync turned into tombstones
     * @param storedRevision the tenant's highest stored revision after the apply, or null without users
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Result(int applied, int ignored, int removed, Long storedRevision) {
    }
}
