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

}
