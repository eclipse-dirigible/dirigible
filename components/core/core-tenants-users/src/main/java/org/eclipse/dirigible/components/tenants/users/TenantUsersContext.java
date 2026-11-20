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
 * @param tenantName the current tenant's name, which is what a person is shown - its id when it has
 *        no name, null in the default tenant (#8724)
 * @param canManage whether the caller may invite, change roles and remove
 * @param canRead whether the caller may see the users
 * @param ownerRole the owner role
 * @param roles the names of the roles an Owner may give, in the order the page offers them
 * @param catalog the same roles with the label and the description the application gives them
 *        (#8728)
 * @param caller who the caller is, as the change requests record them - so a page can mark "you"
 * @param seatLimit the most users the tenant may hold at once, or null for no limit (#8593) - the
 *        page counts the seats taken from the users it lists
 */
record TenantUsersContext(boolean enabled, String tenantId, String tenantName, boolean canManage, boolean canRead, String ownerRole,
        List<String> roles, List<TenantUserRoles.Offer> catalog, String caller, Integer seatLimit) {
}
