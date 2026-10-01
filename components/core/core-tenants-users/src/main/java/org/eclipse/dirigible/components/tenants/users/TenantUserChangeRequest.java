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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The change request {@code tenant.user.change.requested} v1 - one per Owner action: invite a
 * person with roles, set a member's roles, or remove a member. The business tenant travels in the
 * payload, because a {@code global:} destination is stamped with the default tenant.
 *
 * @param requestId minted once per action - the consumer's idempotency key
 * @param type always {@link #TYPE}
 * @param version always {@link #VERSION}
 * @param action what the Owner asked for
 * @param tenantId the session's tenant, never a request field
 * @param appId this application
 * @param email the person, lower-cased and trimmed
 * @param roles the desired set, sorted and without duplicates; absent for a removal
 * @param expectedRevision the user's revision as the Owner's screen showed it; absent for an invite
 * @param requestedBy who asked - an email, audit data only
 * @param requestedAt when, ISO-8601 UTC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"requestId", "type", "version", "action", "tenantId", "appId", "email", "roles", "expectedRevision", "requestedBy",
        "requestedAt"})
record TenantUserChangeRequest(String requestId, String type, int version, Action action, String tenantId, String appId, String email,
        List<String> roles, Long expectedRevision, String requestedBy, String requestedAt) {

    /** The message type. */
    static final String TYPE = "tenant.user.change.requested";

    /** The message version. */
    static final int VERSION = 1;

    /** What the Owner asked for. */
    enum Action {

        /** The person is not a member: add them with the roles. */
        INVITE,

        /** A member: make their roles exactly the roles. */
        UPDATE_ROLES,

        /** Take every role away; the person leaves the tenant. */
        REMOVE
    }
}
