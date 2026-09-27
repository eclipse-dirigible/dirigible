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

/**
 * The request published for an external provisioning system: the envelope
 * {@code user.assignment.requested}, version 1. The business tenant travels here, because a message
 * on a {@code global:} destination is stamped with the default tenant.
 *
 * @param messageId the request's id - the consumer's idempotency key, stored on the user before the
 *        send
 * @param type always {@code user.assignment.requested}
 * @param version always 1
 * @param tenantId the tenant the person is invited into
 * @param appId this application's id
 * @param email the person's email, lower-cased
 * @param role the role, with the configured casing
 * @param requestedBy the owner who asked
 * @param requestedAt when, ISO-8601
 */
record UserAssignmentRequest(String messageId, String type, int version, String tenantId, String appId, String email, String role,
        String requestedBy, String requestedAt) {

    /** The message type. */
    static final String TYPE = "user.assignment.requested";
}
