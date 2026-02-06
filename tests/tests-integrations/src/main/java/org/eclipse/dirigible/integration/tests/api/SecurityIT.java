/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityIT extends IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testPublicEndpoint() throws Exception {
        Set<String> paths = Set.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/login", "/error.html");
        for (String path : paths) {
            mvc.perform(get(path))
               .andExpect(status().is(HttpStatus.OK.value()));
        }

        mvc.perform(get("/.well-known/security.txt"))
           .andExpect(status().is(HttpStatus.NOT_FOUND.value()));

    }

    @Test
    void testProtectedEndpointWithoutAuthentication() throws Exception {
        Set<String> paths = Set.of("/spring-admin", "/actuator/info", "/actuator/sbom", "/actuator/sbom/application");
        for (String path : paths) {
            mvc.perform(get(path))
               .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(username = "user_without_roles", roles = {"SOME_UNUSED_ROLE"})
    void testProtectedEndpointsWithUnauthorizedUser() throws Exception {
        Set<String> paths = Set.of("/actuator/info", "/actuator/sbom", "/actuator/sbom/application");
        for (String path : paths) {
            mvc.perform(get(path))
               .andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(username = "operator", roles = {Roles.RoleNames.OPERATOR})
    void testOperatorEndpointIsAccessible() throws Exception {
        Map<String, HttpStatus> paths = Map.of("/spring-admin", HttpStatus.NOT_FOUND, "/actuator/info", HttpStatus.OK, "/actuator/sbom",
                HttpStatus.OK, "/actuator/sbom/application", HttpStatus.OK);
        for (Map.Entry<String, HttpStatus> entry : paths.entrySet()) {
            mvc.perform(get(entry.getKey()))
               .andExpect(status().is(entry.getValue()
                                           .value()));
        }
    }

    @Test
    @WithMockUser(username = "developer", roles = {Roles.RoleNames.DEVELOPER})
    void testDeveloperEndpointIsAccessible() throws Exception {
        Set<String> paths = Set.of("/services/ide/123", "/websockets/ide/123");
        for (String path : paths) {
            mvc.perform(get(path))
               .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        }
    }

}
