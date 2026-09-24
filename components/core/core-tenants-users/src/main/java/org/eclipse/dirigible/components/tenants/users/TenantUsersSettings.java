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

import java.util.Arrays;
import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;

/**
 * The configuration of tenant users management, read per use so a changed value applies at once.
 */
final class TenantUsersSettings {

    private TenantUsersSettings() {}

    /**
     * The queue invitations are published to.
     *
     * @return the queue, or null
     */
    static String requestQueue() {
        return DirigibleConfig.TENANT_USERS_REQUEST_QUEUE.getStringValue();
    }

    /**
     * The tenant role that may manage users.
     *
     * @return the role
     */
    static String ownerRole() {
        return DirigibleConfig.TENANT_USERS_OWNER_ROLE.getStringValue()
                                                      .trim();
    }

    /**
     * The roles an owner may grant, in configured order.
     *
     * @return the roles
     */
    static List<String> grantableRoles() {
        String configured = DirigibleConfig.TENANT_USERS_ROLES.getStringValue();
        return configured == null ? List.of()
                : Arrays.stream(configured.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .distinct()
                        .toList();
    }
}
