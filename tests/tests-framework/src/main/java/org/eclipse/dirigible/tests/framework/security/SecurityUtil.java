/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.security;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private final UserService userService;
    private final TenantService tenantService;
    private final RoleService roleService;

    SecurityUtil(UserService userService, TenantService tenantService, RoleService roleService) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.roleService = roleService;
    }

    public void createUserInDefaultTenant(String username, String password, String roleName) {
        User user = createUserInDefaultTenant(username, password);

        Role role = roleService.findByName(roleName);
        userService.assignUserRoles(user, role);
    }

    public User createUserInDefaultTenant(String username, String password) {
        DirigibleTestTenant defaultTenant = DirigibleTestTenant.createDefaultTenant();

        String defaultTenantId = tenantService.findBySubdomain(defaultTenant.getSubdomain())
                                              .get()
                                              .getId();
        return createUser(defaultTenantId, username, password);
    }

    public User createUser(String tenantId, String username, String password) {
        return userService.createNewUser(username, password, tenantId);
    }

    public void createUser(String tenantId, String username, String password, String roleName) {
        User user = createUser(tenantId, username, password);

        Role role = roleService.findByName(roleName);
        userService.assignUserRoles(user, role);
    }

    public void assignAllSystemRolesToUser(String username, String tenantId) {
        User user = userService.findUserByUsernameAndTenantId(username, tenantId)
                               .orElseThrow(() -> new IllegalArgumentException(
                                       "Missing user with username [" + username + "] for tenant with id: " + tenantId));

        for (Roles role : Roles.values()) {
            assignRoleToUser(username, tenantId, role.getRoleName());
        }
    }

    public void assignRoleToUser(String username, String tenantId, String roleName) {
        User user = userService.findUserByUsernameAndTenantId(username, tenantId)
                               .orElseThrow(() -> new IllegalArgumentException(
                                       "Missing user with username [" + username + "] for tenant with id: " + tenantId));

        Role role = roleService.findByName(roleName);
        userService.assignUserRoles(user, role);
    }

}
