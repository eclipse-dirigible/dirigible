/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.init;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class SystemRolesInitializer.
 */
@Order(ApplicationReadyEventListeners.SYSTEM_ROLES_INITIALIZER)
@Component
class SystemRolesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemRolesInitializer.class);

    /** The role service. */
    private final RoleService roleService;

    /**
     * Instantiates a new system roles initializer.
     *
     * @param roleService the role service
     */
    SystemRolesInitializer(RoleService roleService) {
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

        for (Roles predefinedRole : Roles.values()) {
            createRole(predefinedRole);
        }

        LOGGER.info("Completed...");
    }

    /**
     * Creates the role.
     *
     * @param predefinedRole the predefined role
     */
    private void createRole(Roles predefinedRole) {
        String roleName = predefinedRole.getRoleName();
        if (roleService.roleExistsByName(roleName)) {
            LOGGER.info("Role with name [{}] already exists. Skipping its creation.", roleName);
            return;
        }
        Role role = new Role();
        role.setType(Role.ARTEFACT_TYPE);
        role.setName(roleName);
        role.setDescription("System role");
        role.setKey("n/a");
        role.setPhase(ArtefactPhase.CREATE);
        role.setLocation("SYSTEM_n/a");

        role.updateKey();

        Role savedRole = roleService.save(role);

        LOGGER.info("Created system role [{}].", savedRole);
    }

}
