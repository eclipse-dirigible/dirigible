/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.init;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.domain.UserRoleAssignment;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The Class AdminUserInitializer.
 */
@ConditionalOnProperty(name = "basic.enabled", havingValue = "true")
@Order(ApplicationReadyEventListeners.ADMIN_USER_INITIALIZER)
@Component
class AdminUserInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserInitializer.class);

    /** The user service. */
    private final UserService userService;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /** The role service. */
    private final RoleService roleService;

    /**
     * Instantiates a new admin user initializer.
     *
     * @param userService the user service
     * @param defaultTenant the default tenant
     * @param roleService the role service
     */
    AdminUserInitializer(UserService userService, @DefaultTenant Tenant defaultTenant, RoleService roleService) {
        this.userService = userService;
        this.defaultTenant = defaultTenant;
        this.roleService = roleService;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Executing...");
        initAdminUser();
        LOGGER.info("Completed.");

    }

    /**
     * Inits the admin user.
     */
    private void initAdminUser() {
        String username = DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();
        String password = DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value();

        Optional<User> existingUser = userService.findUserByUsernameAndTenantId(username, defaultTenant.getId());
        if (existingUser.isPresent()) {
            LOGGER.info("A user with username [{}] for tenant [{}] already exists. Skipping its creation.", username,
                    defaultTenant.getId());
            return;
        }
        User adminUser = userService.createNewUser(username, password, defaultTenant.getId());
        LOGGER.info("Created admin user with username [{}] for tenant with id [{}]", username, defaultTenant.getId());
        for (Roles predefinedRole : Roles.values()) {
            assignRole(adminUser, predefinedRole);
        }
    }

    /**
     * Assign role.
     *
     * @param user the user
     * @param predefinedRole the predefined role
     */
    private void assignRole(User user, Roles predefinedRole) {
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(user);

        String roleName = predefinedRole.getRoleName();
        Role role = roleService.findByName(roleName);
        userService.assignUserRoles(user, role);

        LOGGER.info("Assigned role [{}] to user [{}] in tenant [{}]", roleName, user.getUsername(), user.getTenant()
                                                                                                        .getId());
    }

}
