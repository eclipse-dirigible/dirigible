/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.snowflake;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
class SnowflakeUserManager {

    private final UserService userService;
    private final RoleService roleService;
    private final TenantContext tenantContext;

    SnowflakeUserManager(UserService userService, RoleService roleService, TenantContext tenantContext) {
        this.userService = userService;
        this.roleService = roleService;
        this.tenantContext = tenantContext;
    }

    public Optional<User> findUserByUsername(String username) {
        String currentTenantId = tenantContext.getCurrentTenant()
                                              .getId();
        return findUserByUsernameAndTenantId(username, currentTenantId);
    }

    public Optional<User> findUserByUsernameAndTenantId(String username, String tenantId) {
        return userService.findUserByUsernameAndTenantId(toSnowflakeUsername(username), tenantId);
    }

    private String toSnowflakeUsername(String username) {
        // usernames are passed in uppercase to the applications via header Sf-Context-Current-User
        return username.toUpperCase();
    }

    public User createNewUser(String username) {
        String currentTenantId = tenantContext.getCurrentTenant()
                                              .getId();
        return createNewUser(username, currentTenantId);
    }

    public User createNewUser(String username, String tenantId) {
        String password = UUID.randomUUID()
                              .toString();// password not used in the Snowflake scenario
        return userService.createNewUser(toSnowflakeUsername(username), password, tenantId);
    }

    public void assignUserRoles(User user, String roleName) {
        Role role = roleService.findByName(roleName);
        userService.assignUserRoles(user, role);
    }

    public Set<String> getUserRoleNames(User user) {
        return userService.getUserRoleNames(user);
    }
}
