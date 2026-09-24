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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * The users callback of the tenant provisioning API: what an external provisioning system records
 * for a user, and the merge rules that make recording it idempotent.
 *
 * <p>
 * Driven as a client holding only {@code TENANT_PROVISIONER}, the role a machine-to-machine token
 * carries. The API is switched on in a static {@code @BeforeAll} (its beans are conditional on a
 * value read at context refresh) and the context is dirtied after the class.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationUsersProvisioningApiIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";
    private static final String TENANT_ID = "app-users-it";

    private static final String PROVISIONER_USER = "app-users-it-provisioner";
    private static final String PLAIN_USER = "app-users-it-plain";
    private static final String PASSWORD = "app-users-it-password";
    private static final String TENANT_PROVISIONER = "TENANT_PROVISIONER";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @BeforeAll
    static void enableTheApi() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterAll
    static void disableTheApi() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @BeforeEach
    void registerTheTenant() {
        securityUtil.ensureRole(TENANT_PROVISIONER);
        securityUtil.ensureUserInDefaultTenant(PROVISIONER_USER, PASSWORD, TENANT_PROVISIONER);
        asProvisioner(() -> given().contentType(ContentType.JSON)
                                   .body(Map.of("name", "App users IT"))
                                   .when()
                                   .put(TENANTS_PATH + TENANT_ID)
                                   .then()
                                   .statusCode(org.hamcrest.Matchers.anyOf(equalTo(201), equalTo(200))));
    }

    @Test
    void aFirstOutcomeCreatesTheUserAndASecondRoleMergesIntoIt() {
        asProvisioner(() -> {
            put("First.User@Example.com", "User", "INVITED", "m-1", null).then()
                                                                         .statusCode(201)
                                                                         .body("email", equalTo("first.user@example.com"))
                                                                         .body("status", equalTo("INVITED"))
                                                                         .body("requestState", equalTo("COMPLETED"))
                                                                         .body("roles", hasSize(1))
                                                                         .body("roles[0].role", equalTo("User"))
                                                                         .body("roles[0].grantedBy", equalTo("owner@example.com"))
                                                                         .body("invitedAt", containsString("T"));

            put("first.user@example.com", "Owner", "ASSIGNED", null, null).then()
                                                                          .statusCode(200)
                                                                          .body("status", equalTo("INVITED"))
                                                                          .body("roles.role", containsInAnyOrder("User", "Owner"));
        });
    }

    @Test
    void aFailureNeverRemovesAGrantAndAFailureOfAnotherRequestIsIgnored() {
        asProvisioner(() -> {
            put("second.user@example.com", "User", "ASSIGNED", "m-2", null).then()
                                                                           .statusCode(201);
            put("second.user@example.com", "Owner", "FAILED", "m-0", "an older request").then()
                                                                                        .statusCode(200)
                                                                                        .body("status", equalTo("ASSIGNED"))
                                                                                        .body("requestState", equalTo("COMPLETED"));
            put("second.user@example.com", "Owner", "FAILED", "m-2", "boom").then()
                                                                            .statusCode(200)
                                                                            .body("status", equalTo("ASSIGNED"))
                                                                            .body("requestState", equalTo("FAILED"))
                                                                            .body("errorMessage", equalTo("boom"));
        });
    }

    @Test
    void aFailureForAnUnknownUserIsAFailedUserWithoutRoles() {
        asProvisioner(() -> put("third.user@example.com", "User", "FAILED", "m-3", null).then()
                                                                                        .statusCode(201)
                                                                                        .body("status", equalTo("FAILED"))
                                                                                        .body("roles", hasSize(0)));
    }

    @Test
    void theUsersOfATenantAreListed() {
        asProvisioner(() -> {
            put("listed.user@example.com", "User", "ASSIGNED", "m-4", null).then()
                                                                           .statusCode(201);
            given().when()
                   .get(TENANTS_PATH + TENANT_ID + "/users")
                   .then()
                   .statusCode(200)
                   .body("email", org.hamcrest.Matchers.hasItem("listed.user@example.com"));
        });
    }

    @Test
    void anUnknownTenantIsNotFoundWithTheApisErrorBody() {
        asProvisioner(() -> given().contentType(ContentType.JSON)
                                   .body(body("x@example.com", "User", "ASSIGNED", null, null))
                                   .when()
                                   .put(TENANTS_PATH + "app-users-it-unknown/users")
                                   .then()
                                   .statusCode(404)
                                   .body("status", equalTo(404))
                                   .body("message", containsString("app-users-it-unknown")));
    }

    @Test
    void anInvalidBodyIsABadRequestNamingTheField() {
        asProvisioner(() -> given().contentType(ContentType.JSON)
                                   .body(body("not-an-email", "User", "PENDING", null, null))
                                   .when()
                                   .put(TENANTS_PATH + TENANT_ID + "/users")
                                   .then()
                                   .statusCode(400)
                                   .body("message", containsString("email"))
                                   .body("message", containsString("status")));
    }

    @Test
    void anAuthenticatedUserWithoutTheRoleIsRefused() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TENANTS_PATH + TENANT_ID + "/users")
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }

    private void asProvisioner(Runnable calls) {
        restAssuredExecutor.execute(calls::run, PROVISIONER_USER, PASSWORD);
    }

    private static Response put(String email, String role, String status, String requestId, String errorMessage) {
        return given().contentType(ContentType.JSON)
                      .body(body(email, role, status, requestId, errorMessage))
                      .when()
                      .put(TENANTS_PATH + TENANT_ID + "/users");
    }

    private static Map<String, Object> body(String email, String role, String status, String requestId, String errorMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("role", role);
        body.put("status", status);
        body.put("requestId", requestId);
        body.put("errorMessage", errorMessage);
        body.put("updatedBy", "owner@example.com");
        return body;
    }
}
