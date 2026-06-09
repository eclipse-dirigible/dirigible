/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

/**
 * Regression test for the {@code RolesSynchronizer} → {@code UserService} cleanup chain. When a
 * {@code .roles} artefact is removed from the registry, the synchronizer's {@code cleanupImpl} must
 * publish a {@code RoleDeletionEvent} before issuing the role-row delete; otherwise any existing
 * {@code DIRIGIBLE_USER_ROLE_ASSIGNMENTS} that reference the role trip the FK
 * {@code FK...DIRIGIBLE_USER_ROLE_ASSIGNMENTS → DIRIGIBLE_SECURITY_ROLES} and the role row survives
 * in the DB, out of sync with its missing source file.
 */
class RoleSynchronizerCleanupIT extends IntegrationTest {

    private static final String PROJECT = "role-cleanup-it";
    private static final String ROLE_NAME = "cleanup-role-it";
    private static final String LOCATION = "/" + PROJECT + "/cleanup.roles";
    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION;
    private static final String TEST_USERNAME = "role-cleanup-it-user";
    private static final String TEST_PASSWORD = "irrelevant-password";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Test
    void role_deletion_detaches_user_assignments_and_removes_role() throws Exception {
        // 1. Publish a .roles file declaring a single role and let the synchronizer create it.
        repository.createResource(REGISTRY_PATH, ("[{\"name\": \"" + ROLE_NAME + "\"}]").getBytes(StandardCharsets.UTF_8));
        synchronizationProcessor.forceProcessSynchronizers();

        Role role = roleService.findByName(ROLE_NAME);
        if (role == null) {
            throw new AssertionError("Role [" + ROLE_NAME + "] should have been created by the synchronizer.");
        }

        // 2. Create a user and assign the role — this populates DIRIGIBLE_USER_ROLE_ASSIGNMENTS,
        // the table whose FK to DIRIGIBLE_SECURITY_ROLES used to trip on cleanup.
        String tenantId = tenantService.findBySubdomain(DirigibleTestTenant.createDefaultTenant()
                                                                           .getSubdomain())
                                       .orElseThrow(() -> new AssertionError("Default tenant missing"))
                                       .getId();
        User user = userService.createNewUser(TEST_USERNAME, TEST_PASSWORD, tenantId);
        userService.assignUserRoles(user, role);
        if (!userService.getUserRoleNames(user)
                        .contains(ROLE_NAME)) {
            throw new AssertionError("Pre-condition failed: user does not carry role [" + ROLE_NAME + "].");
        }

        // 3. Remove the .roles file and let the synchronizer reconcile. Without the cleanup chain
        // this is where the FK trips and the role row survives.
        repository.removeResource(REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        // 4. The role row must be gone. RoleService.findByName throws on miss, so probe via the
        // full list which returns an empty result when the row is absent.
        boolean roleStillPresent = roleService.getAll()
                                              .stream()
                                              .anyMatch(r -> ROLE_NAME.equals(r.getName()));
        if (roleStillPresent) {
            throw new AssertionError(
                    "Role [" + ROLE_NAME + "] survived synchronizer cleanup — the FK to DIRIGIBLE_USER_ROLE_ASSIGNMENTS likely tripped.");
        }

        // 5. The assignment row must be gone too. We can only observe this indirectly through the
        // user, since UserRoleAssignmentRepository is package-private.
        if (userService.getUserRoleNames(user)
                       .contains(ROLE_NAME)) {
            throw new AssertionError("User still carries role [" + ROLE_NAME + "] after synchronizer cleanup.");
        }
    }
}
