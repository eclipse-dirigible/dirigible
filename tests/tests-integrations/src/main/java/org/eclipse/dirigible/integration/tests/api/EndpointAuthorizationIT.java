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
import static org.hamcrest.Matchers.not;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end proof that the {@code @RolesAllowed} annotations guarding the platform's endpoints are
 * actually ENFORCED, not merely present.
 * <p>
 * Only a few path prefixes carry role rules in the central URL configuration; every other endpoint
 * under {@code /services/**} is authenticated there and relies on its own {@code @RolesAllowed},
 * which Spring honours only while JSR-250 method security is switched on. When that switch was
 * bound to the basic-authentication configuration, every single-sign-on profile turned it off and
 * the annotations silently stopped guarding anything - the administrative surfaces below were open
 * to any authenticated user, with no error anywhere. This test locks the enforcement in place.
 */
class EndpointAuthorizationIT extends IntegrationTest {

    private static final String PLAIN_USER = "endpoint-authz-it-user";
    private static final String ADMIN_USER = "endpoint-authz-it-admin";
    private static final String PASSWORD = "endpoint-authz-it-password";

    /** Administrative surfaces that must never answer a role-less but authenticated caller. */
    private static final String[] PRIVILEGED_ENDPOINTS = { //
            "/services/data/metadata/", // database metadata (the Database perspective)
            "/services/data/sources", // data source definitions - carry credentials
            "/services/core/configurations", // configuration values
            "/services/security/tenants", // tenant administration
            "/services/core/extensions"}; // extension inventory

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void privileged_endpoints_reject_an_authenticated_user_without_a_platform_role() {
        securityUtil.createUserInDefaultTenant(PLAIN_USER, PASSWORD);

        for (String endpoint : PRIVILEGED_ENDPOINTS) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .get(endpoint)
                                                     .then()
                                                     .statusCode(403),
                    PLAIN_USER, PASSWORD);
        }
    }

    @Test
    void privileged_endpoints_admit_an_administrator() {
        securityUtil.createUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations")
                                                 .then()
                                                 .statusCode(200),
                ADMIN_USER, PASSWORD);
    }

    /**
     * The database export dumps live in the CMS. They were reachable anonymously through the public CMS
     * mapping; now the CMS is served only from the secured path, and the exports folder inside it
     * additionally requires the roles that may produce an export.
     */
    @Test
    void database_exports_are_not_readable_without_an_administrative_role() {
        securityUtil.createUserInDefaultTenant(PLAIN_USER, PASSWORD);

        // the anonymous CMS mapping is gone entirely, so the path resolves to no handler at all
        given().when()
               .get("/public/cms/__EXPORTS/dump.zip")
               .then()
               .statusCode(404);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/cms/__EXPORTS/dump.zip")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);

        // ... and the roles that MAY read an export still get past the guard (the export download in
        // the IDE shell must keep working - this request only fails later, on the missing document)
        securityUtil.createUserInDefaultTenant(ADMIN_USER, PASSWORD, Roles.RoleNames.ADMINISTRATOR);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/cms/__EXPORTS/dump.zip")
                                                 .then()
                                                 .statusCode(not(403)),
                ADMIN_USER, PASSWORD);
    }

    /**
     * A task's variables carry its business payload, so reading them is scoped to the caller's own
     * inbox exactly like acting on the task - a bare task id must not address them.
     */
    @Test
    void task_variables_are_not_readable_for_a_foreign_task() {
        securityUtil.createUserInDefaultTenant(PLAIN_USER, PASSWORD);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/tasks/1234567890/variables")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }
}
