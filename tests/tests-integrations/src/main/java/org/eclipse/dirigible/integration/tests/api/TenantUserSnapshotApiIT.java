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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * The users snapshot endpoint of the tenant provisioning API: how the replica follows the external
 * provisioning system's snapshots - revision ordering, a full replace that keeps the application's
 * own last sign-in, hidden tombstones that an older snapshot cannot revive, and the resync that
 * tombstones only what it did not name and did not outdate.
 *
 * <p>
 * Driven as a client holding only {@code TENANT_PROVISIONER}, the role a machine-to-machine token
 * carries. The API is switched on in a static {@code @BeforeAll} (its beans are conditional on a
 * value read at context refresh) and the context is dirtied after the class.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantUserSnapshotApiIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";
    private static final String TENANT_ID = "it-snap";
    private static final String USERS_PATH = TENANTS_PATH + TENANT_ID + "/users";

    private static final String PROVISIONER_USER = "it-snap-provisioner";
    private static final String PLAIN_USER = "it-snap-plain";
    private static final String PASSWORD = "it-snap-password";
    private static final String TENANT_PROVISIONER = "TENANT_PROVISIONER";

    private static final String OWNER = "bob@acme.test";
    private static final String AT = "2026-09-30T10:00:00Z";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @LocalServerPort
    private int port;

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
                                   .body(Map.of("name", "Snapshot IT"))
                                   .when()
                                   .put(TENANTS_PATH + TENANT_ID)
                                   .then()
                                   .statusCode(anyOf(equalTo(201), equalTo(200))));
    }

    /**
     * The rules in the order a real tenant meets them. One scenario, because each step builds on the
     * revisions the previous one stored.
     */
    @Test
    void theReplicaFollowsTheProvisioningSystemsRevisions() {
        asProvisioner(() -> {
            Map<String, Object> ann = user("Ann@Example.com", 10, "ASSIGNED", role("User", "GRANTED", OWNER, AT));
            Map<String, Object> ben =
                    user("ben@example.com", 11, "INVITED", role("User", "GRANTED", OWNER, AT), role("Owner", "GRANTED", OWNER, AT));

            // 1. two users are applied, and the watermark is the higher revision
            sync(false, 11, ann, ben).body("applied", equalTo(2))
                                     .body("ignored", equalTo(0))
                                     .body("storedRevision", equalTo(11));

            // 2. both read back, lower-cased, with ISO timestamps and the roles by name
            users(false).body("$", hasSize(2))
                        .body("email", contains("ann@example.com", "ben@example.com"))
                        .body("[0].invitedAt", endsWith("Z"))
                        .body("[1].roles.role", contains("Owner", "User"));

            // 3. the same body again is ignored, never an error
            sync(false, 11, ann, ben).body("applied", equalTo(0))
                                     .body("ignored", equalTo(2));

            // 4. a change in progress shows on the roles it touches; a refused change as the error
            sync(false, 12,
                    user("ann@example.com", 12, "ASSIGNED", role("Owner", "ADDING", null, null), role("User", "REMOVING", OWNER, AT)));
            users(false).body("find { it.email == 'ann@example.com' }.roles.find { it.role == 'Owner' }.state", equalTo("ADDING"))
                        .body("find { it.email == 'ann@example.com' }.roles.find { it.role == 'Owner' }.grantedAt", nullValue())
                        .body("find { it.email == 'ann@example.com' }.roles.find { it.role == 'User' }.state", equalTo("REMOVING"));
            Map<String, Object> refused = user("ann@example.com", 13, "ASSIGNED", role("User", "GRANTED", OWNER, AT));
            refused.put("lastError", Map.of("code", "LAST_OWNER", "message", "the tenant would have no owner"));
            sync(false, 13, refused);
            users(false).body("find { it.email == 'ann@example.com' }.roles", hasSize(1))
                        .body("find { it.email == 'ann@example.com' }.lastError.code", equalTo("LAST_OWNER"));

            // 5. a removal hides the user, and the tombstone is still there for diagnostics
            sync(false, 14, user("ben@example.com", 14, "REMOVED"));
            users(false).body("email", contains("ann@example.com"));
            users(true).body("find { it.email == 'ben@example.com' }.status", equalTo("REMOVED"))
                       .body("find { it.email == 'ben@example.com' }.roles", hasSize(0));

            // 6. a late snapshot older than the removal cannot bring the user back
            sync(false, 13, user("ben@example.com", 13, "ASSIGNED", role("User", "GRANTED", OWNER, AT))).body("ignored", equalTo(1));
            users(true).body("find { it.email == 'ben@example.com' }.status", equalTo("REMOVED"));

            // 7. a resync tombstones the live user it does not name; the existing tombstone is not counted
            sync(true, 20, user("carl@example.com", 20, "ASSIGNED", role("User", "GRANTED", OWNER, AT))).body("applied", equalTo(1))
                                                                                                        .body("removed", equalTo(1))
                                                                                                        .body("storedRevision",
                                                                                                                equalTo(20));
            users(false).body("email", contains("carl@example.com"));
            users(true).body("find { it.email == 'ann@example.com' }.status", equalTo("REMOVED"))
                       .body("find { it.email == 'ann@example.com' }.revision", equalTo(20));
        });
    }

    /**
     * An erasure: the tenant forgets one person for good - the tombstone, the platform user of that
     * address in any case, and a live user's role rows - while the other users and the next snapshot's
     * rules stay as they were. Revisions above 20 keep these users out of the other scenario's resync.
     */
    @Test
    void aForgottenUserIsGoneForGoodTombstoneAndPlatformUserIncluded() throws SQLException {
        securityUtil.createUser(TENANT_ID, "Dora@Example.com", PASSWORD);
        asProvisioner(() -> {
            // 1. two users, then one of them removed - a tombstone
            sync(false, 31, user("dora@example.com", 30, "ASSIGNED", role("User", "GRANTED", OWNER, AT)),
                    user("eve@example.com", 31, "ASSIGNED", role("User", "GRANTED", OWNER, AT), role("Owner", "GRANTED", OWNER, AT)));
            sync(false, 32, user("dora@example.com", 32, "REMOVED"));
            users(true).body("find { it.email == 'dora@example.com' }.status", equalTo("REMOVED"));

            // 2. forgetting the tombstone: 204, and it is no longer listed, even with the tombstones
            forget(TENANT_ID, "Dora@Example.com").statusCode(204);
            users(true).body("find { it.email == 'dora@example.com' }", nullValue())
                       .body("find { it.email == 'eve@example.com' }.roles", hasSize(2));

            // 3. again: 204 - idempotent
            forget(TENANT_ID, "dora@example.com").statusCode(204);

            // 4. a snapshot naming the other user still applies by the usual rules
            sync(false, 33,
                    user("eve@example.com", 33, "ASSIGNED", role("User", "GRANTED", OWNER, AT), role("Owner", "GRANTED", OWNER, AT))).body(
                            "applied", equalTo(1));
        });
        assertEquals(0, replicaRows("dora@example.com"));
        assertEquals(0, platformUsers("dora@example.com"), "the platform user [Dora@Example.com] must be deleted, ignoring case");
        assertEquals(1, replicaRows("eve@example.com"));

        // a live user goes with its role rows
        int roleRowsBefore = roleRows();
        asProvisioner(() -> forget(TENANT_ID, "eve@example.com").statusCode(204));
        assertEquals(0, replicaRows("eve@example.com"));
        assertEquals(roleRowsBefore - 2, roleRows());

        // the refusals, in the API's error body
        asProvisioner(() -> {
            forget("it-snap-unknown", "dora@example.com").statusCode(404)
                                                         .body("status", equalTo(404));
            forget(TENANT_ID, "not-an-email").statusCode(400)
                                             .body("message", not(blankOrNullString()));
            forget("default-tenant", "dora@example.com").statusCode(400);
        });
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);
        restAssuredExecutor.execute(() -> forget(TENANT_ID, "eve@example.com").statusCode(403), PLAIN_USER, PASSWORD);
        given().port(port)
               .when()
               .delete(USERS_PATH + "/{email}", "eve@example.com")
               .then()
               .statusCode(401);
    }

    @Test
    void anUnknownTenantIsNotFoundWithTheApisErrorBody() {
        asProvisioner(() -> given().contentType(ContentType.JSON)
                                   .body(body(false, 1, user("ann@example.com", 1, "INVITED")))
                                   .when()
                                   .put(TENANTS_PATH + "it-snap-unknown/users")
                                   .then()
                                   .statusCode(404)
                                   .body("status", equalTo(404))
                                   .body("error", equalTo("Not Found"))
                                   .body("message", containsString("it-snap-unknown")));
    }

    @Test
    void oneBadRequestNamesEveryOffendingField() {
        asProvisioner(() -> given().contentType(ContentType.JSON)
                                   .body(body(false, 1, user("not-an-email", 1, "INVITED", role("x".repeat(101), "GRANTED", OWNER, AT))))
                                   .when()
                                   .put(USERS_PATH)
                                   .then()
                                   .statusCode(400)
                                   .body("status", equalTo(400))
                                   .body("message", containsString("users[0].email"))
                                   .body("message", containsString("users[0].roles[0].role")));
    }

    @Test
    void anAuthenticatedUserWithoutTheRoleIsRefused() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(USERS_PATH)
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }

    private void asProvisioner(Runnable calls) {
        restAssuredExecutor.execute(calls::run, PROVISIONER_USER, PASSWORD);
    }

    @SafeVarargs
    private static ValidatableResponse sync(boolean complete, long revision, Map<String, Object>... users) {
        return given().contentType(ContentType.JSON)
                      .body(body(complete, revision, users))
                      .when()
                      .put(USERS_PATH)
                      .then()
                      .statusCode(200);
    }

    private static ValidatableResponse forget(String tenantId, String email) {
        return given().when()
                      .delete(TENANTS_PATH + tenantId + "/users/{email}", email)
                      .then();
    }

    private int replicaRows(String email) throws SQLException {
        return count("SELECT COUNT(*) FROM DIRIGIBLE_TENANT_USERS WHERE TENUSER_TENANT_ID = ? AND TENUSER_EMAIL = ?", TENANT_ID, email);
    }

    private int platformUsers(String email) throws SQLException {
        return count("SELECT COUNT(*) FROM DIRIGIBLE_USERS WHERE USER_TENANT_ID = ? AND LOWER(USER_USERNAME) = ?", TENANT_ID, email);
    }

    private int roleRows() throws SQLException {
        return count("SELECT COUNT(*) FROM DIRIGIBLE_TENANT_USER_ROLES");
    }

    private int count(String sql, String... parameters) throws SQLException {
        try (Connection connection = dataSourcesManager.getSystemDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static ValidatableResponse users(boolean includeRemoved) {
        return given().when()
                      .get(USERS_PATH + "?includeRemoved=" + includeRemoved)
                      .then()
                      .statusCode(200);
    }

    @SafeVarargs
    private static Map<String, Object> body(boolean complete, long revision, Map<String, Object>... users) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("complete", complete);
        body.put("revision", revision);
        body.put("users", List.of(users));
        return body;
    }

    @SafeVarargs
    private static Map<String, Object> user(String email, long revision, String status, Map<String, Object>... roles) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("email", email);
        user.put("revision", revision);
        user.put("status", status);
        user.put("roles", new ArrayList<>(List.of(roles)));
        user.put("invitedBy", OWNER);
        user.put("invitedAt", AT);
        user.put("lastChangedBy", OWNER);
        user.put("lastChangedAt", AT);
        return user;
    }

    private static Map<String, Object> role(String role, String state, String grantedBy, String grantedAt) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", role);
        entry.put("state", state);
        entry.put("grantedBy", grantedBy);
        entry.put("grantedAt", grantedAt);
        return entry;
    }
}
