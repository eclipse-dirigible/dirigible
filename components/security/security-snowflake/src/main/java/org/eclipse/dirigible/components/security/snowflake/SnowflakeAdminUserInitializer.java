/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.snowflake;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.domain.UserRoleAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Profile("snowflake")
@Order(ApplicationReadyEventListeners.ADMIN_USER_INITIALIZER)
@Component
class SnowflakeAdminUserInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeAdminUserInitializer.class);

    private final Tenant defaultTenant;

    private final SnowflakeUserManager snowflakeUserManager;

    SnowflakeAdminUserInitializer(@DefaultTenant Tenant defaultTenant, SnowflakeUserManager snowflakeUserManager) {
        this.defaultTenant = defaultTenant;
        this.snowflakeUserManager = snowflakeUserManager;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Executing...");
        initAdminUser();
        LOGGER.info("Completed.");

    }

    private void initAdminUser() {
        Optional<String> optionalUsername = getSnowflakeAdminUsername();
        if (optionalUsername.isEmpty()) {
            LOGGER.warn("Admin user will not be initialized");
            return;
        }
        String username = optionalUsername.get();

        Optional<User> existingUser = snowflakeUserManager.findUserByUsernameAndTenantId(username, defaultTenant.getId());
        if (existingUser.isPresent()) {
            LOGGER.info("A user with username [{}] for tenant [{}] already exists. Skipping its initialization.", username,
                    defaultTenant.getId());
            return;
        }
        User adminUser = snowflakeUserManager.createNewUser(username, defaultTenant.getId());
        LOGGER.info("Created admin user with username [{}] for tenant with id [{}]", adminUser.getUsername(), adminUser.getTenant()
                                                                                                                       .getId());
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
        snowflakeUserManager.assignUserRoles(user, roleName);

        LOGGER.info("Assigned role [{}] to user [{}] in tenant [{}]", roleName, user.getUsername(), user.getTenant()
                                                                                                        .getId());
    }

    private Optional<String> getSnowflakeAdminUsername() {
        String username = DirigibleConfig.SNOWFLAKE_ADMIN_USERNAME.getStringValue();
        if (StringUtils.isBlank(username)) {
            LOGGER.warn("Missing snowflake admin username in configuration [{}].", DirigibleConfig.SNOWFLAKE_ADMIN_USERNAME.getKey());
            return Optional.empty();
        }
        return Optional.of(username);
    }

}
