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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * Registering a tenant from the outside, and the promises the registration makes.
 *
 * <p>
 * The API is switched on in a static {@code @BeforeAll}, which is what makes it take effect: its
 * beans are conditional on a configuration value read when the Spring context is refreshed, and
 * that happens after every {@code @BeforeAll} has run. The context is dirtied after the class, so
 * the switch does not leak into the next one.
 *
 * <p>
 * Activation and everything it materializes live in {@code TenantActivationIT}; this class covers
 * what a provisioner does before it - and the one thing that must NOT happen in between, which is
 * the platform's own provisioner adopting a tenant somebody else is provisioning.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantProvisioningApiIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";
    private static final String PLATFORM_TENANTS_PATH = "/services/security/tenants/";

    private static final String TENANT_ID = "provisioning-api-it";
    private static final String OTHER_TENANT_ID = "provisioning-api-it-other";
    private static final String ROLLBACK_TENANT_ID = "provisioning-api-it-rollback";
    private static final String UNPROTECTED_TENANT_ID = "provisioning-api-it-authz";

    private static final String PLAIN_USER = "provisioning-api-it-plain";
    private static final String PROVISIONER_USER = "provisioning-api-it-provisioner";
    private static final String PASSWORD = "provisioning-api-it-password";

    /** Deliberately not in the Roles enum - it is carried by a machine-to-machine token. */
    private static final String TENANT_PROVISIONER = "TENANT_PROVISIONER";

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private Scheduler scheduler;

    @BeforeAll
    static void enableTheApi() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterAll
    static void disableTheApi() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @Test
    void aTenantIsRegisteredPendingActivationAndCanBeReadBack() {
        restAssuredExecutor.execute(() -> {
            register(TENANT_ID, "Acme Ltd").then()
                                           .statusCode(201)
                                           .body("id", equalTo(TENANT_ID))
                                           .body("name", equalTo("Acme Ltd"))
                                           .body("subdomain", equalTo(TENANT_ID))
                                           .body("status", equalTo(TenantStatus.PENDING_ACTIVATION.name()))
                                           .body("initialization.status", equalTo("NOT_STARTED"));

            given().when()
                   .get(TENANTS_PATH + TENANT_ID)
                   .then()
                   .statusCode(200)
                   .body("status", equalTo(TenantStatus.PENDING_ACTIVATION.name()))
                   .body("initialization.status", equalTo("NOT_STARTED"));
        });
    }

    /**
     * The provisioner that calls this API is a retrying process, so the same registration arriving
     * twice has to converge rather than collide.
     */
    @Test
    void registeringTheSameTenantAgainUpdatesItInsteadOfFailing() {
        restAssuredExecutor.execute(() -> {
            register(OTHER_TENANT_ID, "Globex").then()
                                               .statusCode(201);

            register(OTHER_TENANT_ID, "Globex Corporation").then()
                                                           .statusCode(200)
                                                           .body("name", equalTo("Globex Corporation"))
                                                           .body("status", equalTo(TenantStatus.PENDING_ACTIVATION.name()));
        });
    }

    /**
     * Every refusal has to say why. The caller is a program that must act differently on each one, and
     * the operator reading its logs learns nothing from a bare status either.
     */
    /**
     * Two tenants may carry the same display name - two customers really can both be "Acme Ltd".
     *
     * <p>
     * A tenant is an artefact, and an artefact's unique key is {@code type:location:name}. Registering
     * under a constant location made the display name the unique part, so the second tenant of that
     * name hit {@code UK_DIRIGIBLE_TENANTS_ARTEFACT_KEY} and the caller got an unhandled 500. Asserted
     * against the real index rather than against the composed key, since that is where it failed.
     */
    @Test
    void twoTenantsMayShareADisplayName() {
        restAssuredExecutor.execute(() -> {
            register("provisioning-api-it-twin-a", "Acme Ltd").then()
                                                              .statusCode(201);
            register("provisioning-api-it-twin-b", "Acme Ltd").then()
                                                              .statusCode(201);
        });

        assertTrue(tenantService.findById("provisioning-api-it-twin-a")
                                .isPresent());
        assertTrue(tenantService.findById("provisioning-api-it-twin-b")
                                .isPresent());
    }

    /** Renaming a tenant onto a name another tenant already uses is likewise not a collision. */
    @Test
    void aTenantMayBeRenamedOntoAnotherTenantsName() {
        restAssuredExecutor.execute(() -> {
            register("provisioning-api-it-rename-a", "Original").then()
                                                                .statusCode(201);
            register("provisioning-api-it-rename-b", "Other").then()
                                                             .statusCode(201);

            register("provisioning-api-it-rename-b", "Original").then()
                                                                .statusCode(200)
                                                                .body("name", equalTo("Original"));
        });
    }

    @Test
    void anIdThatCannotBeAnIdentityProviderGroupSegmentIsRefused() {
        restAssuredExecutor.execute(() -> {
            register("acme.corp", "Dotted").then()
                                           .statusCode(400)
                                           .body("message", containsString("acme.corp"));
            register("-acme", "Leading hyphen").then()
                                               .statusCode(400);
        });
    }

    @Test
    void aBodyWithoutANameIsRefusedNamingTheField() {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of())
                                                 .when()
                                                 .put(TENANTS_PATH + "provisioning-api-it-nameless")
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("name")));
    }

    @Test
    void anUnknownTenantIsNotFound() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TENANTS_PATH + "provisioning-api-it-nowhere")
                                                 .then()
                                                 .statusCode(404));
    }

    /** The subdomain column is unique platform-wide; the conflict has to name the tenant holding it. */
    @Test
    void aSubdomainHeldByAnotherTenantIsAConflict() {
        restAssuredExecutor.execute(() -> {
            registerWithSubdomain("provisioning-api-it-sub-a", "A", "provisioning-api-it-shared").then()
                                                                                                 .statusCode(201);

            registerWithSubdomain("provisioning-api-it-sub-b", "B", "provisioning-api-it-shared").then()
                                                                                                 .statusCode(409)
                                                                                                 .body("message", containsString(
                                                                                                         "provisioning-api-it-sub-a"));
        });
    }

    /**
     * The invariant the whole external flow rests on: a tenant somebody else is provisioning must not
     * be adopted by the platform's own provisioner, which would create a database user and a schema of
     * its own beside the ones that already exist.
     *
     * <p>
     * A genuinely INITIAL tenant is provisioned alongside it, which is what makes the assertion sound:
     * it proves the provisioning run actually happened and completed, so the externally owned tenant
     * being untouched is a decision rather than a race the test won.
     */
    @Test
    void theBuiltInProvisionerLeavesAnExternallyOwnedTenantAlone() throws SchedulerException {
        String externallyOwned = "provisioning-api-it-untouched";
        restAssuredExecutor.execute(() -> register(externallyOwned, "Untouched").then()
                                                                                .statusCode(201));

        DirigibleTestTenant ownedByThePlatform = new DirigibleTestTenant("provisioning-api-it-builtin");
        createTenants(ownedByThePlatform);
        scheduler.triggerJob(JobKey.jobKey("TenantsProvisioningJob", "system"));
        waitForTenantProvisioning(ownedByThePlatform);

        Tenant tenant = tenantService.findById(externallyOwned)
                                     .orElseThrow();
        assertEquals(TenantStatus.PENDING_ACTIVATION, tenant.getStatus(),
                "the built-in provisioner must not activate a tenant it does not own");
        assertFalse(dataSourceService.findOptionalByName(externallyOwned + "_DefaultDB")
                                     .isPresent(),
                "the built-in provisioner must not create a data source for a tenant it does not own");
    }

    /** The rollback path: a provisioner that gives up before activating can take the tenant back. */
    @Test
    void aTenantThatWasNeverActivatedCanBeDeleted() {
        restAssuredExecutor.execute(() -> {
            register(ROLLBACK_TENANT_ID, "Rolled back").then()
                                                       .statusCode(201);

            given().when()
                   .delete(PLATFORM_TENANTS_PATH + ROLLBACK_TENANT_ID)
                   .then()
                   .statusCode(204);

            given().when()
                   .get(TENANTS_PATH + ROLLBACK_TENANT_ID)
                   .then()
                   .statusCode(404);
        });
        assertTrue(tenantService.findById(ROLLBACK_TENANT_ID)
                                .isEmpty());
    }

    @Test
    void anAuthenticatedUserWithoutARoleIsRefused() {
        securityUtil.ensureUserInDefaultTenant(PLAIN_USER, PASSWORD);

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("name", "Refused"))
                                                 .when()
                                                 .put(TENANTS_PATH + UNPROTECTED_TENANT_ID)
                                                 .then()
                                                 .statusCode(403),
                PLAIN_USER, PASSWORD);
    }

    /**
     * The role a machine-to-machine client actually presents - it holds nothing else, which is why the
     * API needed a URL prefix outside the platform's own role-gated ones.
     */
    @Test
    void aClientHoldingOnlyTheProvisionerRoleIsAdmitted() {
        securityUtil.ensureRole(TENANT_PROVISIONER);
        securityUtil.ensureUserInDefaultTenant(PROVISIONER_USER, PASSWORD, TENANT_PROVISIONER);

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("name", "Admitted"))
                                                 .when()
                                                 .put(TENANTS_PATH + "provisioning-api-it-m2m")
                                                 .then()
                                                 .statusCode(201),
                PROVISIONER_USER, PASSWORD);
    }

    private static io.restassured.response.Response register(String tenantId, String name) {
        return given().contentType(ContentType.JSON)
                      .body(Map.of("name", name))
                      .when()
                      .put(TENANTS_PATH + tenantId);
    }

    private static io.restassured.response.Response registerWithSubdomain(String tenantId, String name, String subdomain) {
        return given().contentType(ContentType.JSON)
                      .body(Map.of("name", name, "subdomain", subdomain))
                      .when()
                      .put(TENANTS_PATH + tenantId);
    }
}
