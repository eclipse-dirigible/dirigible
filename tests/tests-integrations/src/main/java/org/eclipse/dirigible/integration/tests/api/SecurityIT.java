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

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add ~10s of boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SecurityIT extends IntegrationTest {

    /** Public static paths served by the framework rather than by an application endpoint. */
    private static final List<String> PUBLIC_STATIC_PATHS = List.of("/webjars/alpinejs/dist/cdn.min.js", "/favicon.ico");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

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
        Set<String> paths = Set.of("/spring-admin", "/actuator/info", "/actuator/sbom", "/actuator/sbom/application",
                "/services/native-apps", "/services/native-apps/123");
        for (String path : paths) {
            mvc.perform(get(path))
               .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(username = "user_without_roles", roles = {"SOME_UNUSED_ROLE"})
    void testProtectedEndpointsWithUnauthorizedUser() throws Exception {
        Set<String> paths = Set.of("/actuator/info", "/actuator/sbom", "/actuator/sbom/application", "/services/native-apps",
                "/services/native-apps/123");
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

        mvc.perform(get("/services/native-apps"))
           .andExpect(status().is(anyOf(equalTo(HttpStatus.OK.value()), equalTo(HttpStatus.NOT_FOUND.value()))));
    }

    @Test
    @WithMockUser(username = "developer", roles = {Roles.RoleNames.DEVELOPER})
    void testDeveloperEndpointIsAccessible() throws Exception {
        Map<String, HttpStatus> paths = Map.of("/services/ide/123", HttpStatus.NOT_FOUND, "/websockets/ide/123", HttpStatus.NOT_FOUND);
        for (Map.Entry<String, HttpStatus> entry : paths.entrySet()) {
            mvc.perform(get(entry.getKey()))
               .andExpect(status().is(entry.getValue()
                                           .value()));
        }

        mvc.perform(get("/services/native-apps"))
           .andExpect(status().is(anyOf(equalTo(HttpStatus.OK.value()), equalTo(HttpStatus.NOT_FOUND.value()))));
    }

    @Test
    @WithMockUser(username = "administrator", roles = {Roles.RoleNames.ADMINISTRATOR})
    void testAdministratorCanReadNativeAppsManagement() throws Exception {
        mvc.perform(get("/services/native-apps"))
           .andExpect(status().is(anyOf(equalTo(HttpStatus.OK.value()), equalTo(HttpStatus.NOT_FOUND.value()))));
    }

    /**
     * Presenting valid credentials must never change the outcome of a public static path. It did: those
     * paths used to be opted out of the tenant execution scope, so the basic authentication filter ran
     * unscoped, failed to resolve the user against a tenant, and the basic entry point answered 401 to
     * credentials that authenticate everywhere else.
     */
    @Test
    void testCredentialsDoNotBreakPublicStaticPaths() {
        restAssuredExecutor.execute(() -> {
            for (String path : PUBLIC_STATIC_PATHS) {
                int anonymousStatus = given().auth()
                                             .none()
                                             .when()
                                             .get(path)
                                             .thenReturn()
                                             .statusCode();
                int authenticatedStatus = given().when()
                                                 .get(path)
                                                 .thenReturn()
                                                 .statusCode();

                assertThat(anonymousStatus).as("Anonymous status of the public path [%s]", path)
                                           .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
                assertThat(authenticatedStatus).as("Authenticated status of the public path [%s]", path)
                                               .isEqualTo(anonymousStatus);
            }
        });
    }

}
