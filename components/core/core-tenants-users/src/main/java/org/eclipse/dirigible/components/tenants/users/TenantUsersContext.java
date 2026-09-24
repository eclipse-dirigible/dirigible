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

/**
 * What a page needs to decide whether to offer users management.
 *
 * @param enabled always true - the endpoint exists only when the feature does
 * @param tenantId the current tenant, or null in the default tenant
 * @param canManage whether the caller may invite
 * @param canRead whether the caller may see the users
 * @param ownerRole the owner role
 * @param roles the grantable roles
 */
record TenantUsersContext(boolean enabled, String tenantId, boolean canManage, boolean canRead, String ownerRole, List<String> roles) {
}
