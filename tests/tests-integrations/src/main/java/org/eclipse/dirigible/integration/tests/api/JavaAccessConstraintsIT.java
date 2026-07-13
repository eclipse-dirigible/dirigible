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

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for HTTP access constraints ({@code *.access}) on client Java endpoints served
 * under {@code /services/java/...}: a role constraint synchronized from the registry must deny a
 * user without the role (403) and admit a user with it (200) as soon as the synchronization round
 * completes.
 */
class JavaAccessConstraintsIT extends IntegrationTest {

    /** Project segment used for all sources in this IT. */
    private static final String PROJECT = "java-access-it";

    /** Registry path of the client Java handler. */
    private static final String SOURCE_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/Hello.java";

    /** Registry path of the access constraint artefact. */
    private static final String ACCESS_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/security/demo.access";

    /** Endpoint where the {@code demo.Hello} handler answers. */
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/demo/Hello";

    private static final String ADMIN_USER = "java-access-it-admin";
    private static final String NON_ADMIN_USER = "java-access-it-developer";
    private static final String PASSWORD = "java-access-it-password";

    /**
     * Wait cap for the first request after the handler is synchronized - the in-process compile +
     * class-define cycle can finish shortly after {@code forceProcessSynchronizers()} returns.
     */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    private static final String HANDLER_SOURCE = """
            package demo;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletResponse;
            import org.eclipse.dirigible.engine.java.handler.JavaHandler;
            public class Hello implements JavaHandler {
                @Override
                public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    response.setContentType("application/json");
                    response.getWriter().write("{\\"message\\": \\"hello\\"}");
                }
            }
            """;

    /** The constraint path is registry-relative - the filter strips the /services/java prefix. */
    private static final String ACCESS_CONSTRAINT = """
            {
                "constraints": [
                    {
                        "path": "/%s/**",
                        "method": "*",
                        "scope": "HTTP",
                        "roles": [
                            "%s"
                        ]
                    }
                ]
            }
            """.formatted(PROJECT, Roles.RoleNames.ADMINISTRATOR);

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void access_constraints_are_enforced_for_java_endpoints() {
        securityUtil.createUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        securityUtil.createUserInDefaultTenant(NON_ADMIN_USER, PASSWORD, Roles.RoleNames.DEVELOPER);

        // without a constraint any authenticated user can call the handler
        repository.createResource(SOURCE_REGISTRY_PATH, HANDLER_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
        assertEndpointStatusEventually(NON_ADMIN_USER, 200);

        // the constraint restricts the project to ADMINISTRATOR; it must be enforced as soon as the
        // forced synchronization returns - no retry window for the 403
        repository.createResource(ACCESS_REGISTRY_PATH, ACCESS_CONSTRAINT.getBytes(StandardCharsets.UTF_8), false, "application/json",
                true);
        synchronizationProcessor.forceProcessSynchronizers();
        assertEndpointStatus(NON_ADMIN_USER, 403);
        assertEndpointStatus(ADMIN_USER, 200);
    }

    private void assertEndpointStatusEventually(String user, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(expectedStatus),
                user, PASSWORD, ASSERTION_TIMEOUT_SECONDS);
    }

    private void assertEndpointStatus(String user, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(expectedStatus),
                user, PASSWORD);
    }

}
