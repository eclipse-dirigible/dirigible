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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.integration.tests.support.TenantUsersTestSupport;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * A role taken out of a {@code .roles} file that stays is removed on the next synchronization
 * (#8854). The processor cleans up an artefact only when its source file is gone, so before the fix
 * the row of the removed role stayed - and, marked assignable, stayed in the roles a tenant's
 * people are offered. The removed row goes the way of a removed file's roles: the user assignments
 * of that row are detached, and a role of the same name declared in another file keeps that file's
 * row.
 *
 * <p>
 * The catalog is read where the provisioning system reads it,
 * {@code GET services/tenant-provisioning/user-roles}; the API's condition is read at context
 * refresh, so it is switched on before the context starts and the context is dirtied after the
 * class.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoleSynchronizerUndeclaredRoleIT extends IntegrationTest {

    private static final String PROJECT = "role-undeclared-it";
    private static final String APP_LOCATION = "/" + PROJECT + "/app.roles";
    private static final String DEFAULT_LOCATION = "/" + PROJECT + "/default-roles.roles";
    private static final String KEPT = "undeclared-it-kept";
    private static final String REMOVED = "undeclared-it-removed";
    private static final String MOVED = "undeclared-it-moved";
    private static final String USER_ROLES = "/services/tenant-provisioning/user-roles";
    private static final String TEST_USERNAME = "role-undeclared-it-user";
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

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void enableTheProvisioningApi() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterAll
    static void disableTheProvisioningApi() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @Test
    void aRoleTakenOutOfALivingFileIsRemovedAndNoLongerOffered() throws Exception {
        // 1. The application's file marks two roles assignable; the generated file declares one of
        // them again, without the mark - the shape an application has before it moves a role out.
        repository.createResource(registryPath(APP_LOCATION), bytes("[{\"name\": \"" + KEPT + "\"}, {\"name\": \"" + REMOVED
                + "\", \"assignable\": true}, {\"name\": \"" + MOVED + "\", \"assignable\": true}]"));
        repository.createResource(registryPath(DEFAULT_LOCATION), bytes("[{\"name\": \"" + MOVED + "\"}]"));
        synchronizationProcessor.forceProcessSynchronizers();

        catalog().andExpect(jsonPath("$.roles[*].name", hasItems(REMOVED, MOVED)));
        assertEquals(List.of(APP_LOCATION, DEFAULT_LOCATION), locationsOf(MOVED));

        // 2. A user holds the role about to be removed.
        String tenantId = tenantService.findBySubdomain(DirigibleTestTenant.createDefaultTenant()
                                                                           .getSubdomain())
                                       .orElseThrow(() -> new AssertionError("Default tenant missing"))
                                       .getId();
        User user = userService.createNewUser(TEST_USERNAME, TEST_PASSWORD, tenantId);
        userService.assignUserRoles(user, roleService.findByKey(roleKey(APP_LOCATION, REMOVED)));
        assertTrue(userService.getUserRoleNames(user)
                              .contains(REMOVED),
                "Pre-condition failed: the user does not hold [" + REMOVED + "]");

        // 3. The same file, republished without the two marked roles.
        repository.getResource(registryPath(APP_LOCATION))
                  .setContent(bytes("[{\"name\": \"" + KEPT + "\"}]"));
        synchronizationProcessor.forceProcessSynchronizers();

        // 4. The removed role has no row left, the moved one keeps only the generated file's unmarked
        // row, and the role the file still declares stays.
        assertTrue(locationsOf(REMOVED).isEmpty(), "Role [" + REMOVED + "] survived its removal from " + APP_LOCATION);
        assertEquals(List.of(DEFAULT_LOCATION), locationsOf(MOVED));
        assertFalse(roleService.findByKey(roleKey(DEFAULT_LOCATION, MOVED))
                               .isAssignable());
        assertEquals(List.of(APP_LOCATION), locationsOf(KEPT));

        // 5. Neither is offered any more, and the user's assignment of the removed row is detached.
        catalog().andExpect(jsonPath("$.roles[*].name", not(hasItem(REMOVED))))
                 .andExpect(jsonPath("$.roles[*].name", not(hasItem(MOVED))));
        assertFalse(userService.getUserRoleNames(user)
                               .contains(REMOVED),
                "The user still holds [" + REMOVED + "] after its removal");
    }

    private ResultActions catalog() throws Exception {
        return mvc.perform(get(USER_ROLES).with(authentication(TenantUsersTestSupport.provisioner())))
                  .andExpect(status().isOk());
    }

    private List<String> locationsOf(String name) {
        return roleService.getAll()
                          .stream()
                          .filter(role -> name.equals(role.getName()))
                          .map(Role::getLocation)
                          .sorted()
                          .toList();
    }

    private static String registryPath(String location) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
    }

    private static String roleKey(String location, String name) {
        return Role.ARTEFACT_TYPE + Role.KEY_SEPARATOR + location + Role.KEY_SEPARATOR + name;
    }

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
