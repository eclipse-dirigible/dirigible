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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
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
 * Activating a tenant initializes that tenant, and leaves every tenant already initialized exactly
 * as it was (#7799).
 *
 * <p>
 * Activation used to blank the checksums of every per-tenant definition and run the whole
 * synchronization again, for every tenant. That re-altered every tenant's tables and re-imported
 * the seed rows a tenant had deleted. And since the per-tenant work shared one artefact row, the
 * activation status reported one tenant's failure for all of them - or hid it behind a later
 * tenant's success (#7776).
 *
 * <p>
 * The test plays the external provisioner for three tenants, as {@code TenantActivationIT} does for
 * one, against a project with a seeded per-tenant table. What it asserts is the data itself: a
 * deleted seed row of one tenant stays deleted while another tenant is activated.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class TenantScopedInitializationIT extends IntegrationTest {

    private static final String TENANTS_PATH = "/services/tenant-provisioning/tenants/";

    private static final String PROJECT = "tenant-scoped-init-it";
    private static final String TABLE = "TENANT_SCOPED_INIT_ORDERS";

    /** Already initialized when another tenant is activated - must not be touched by it. */
    private static final ExternalTenant TENANT_A = new ExternalTenant("tenant-scoped-a", "TENANTSCOPEDA", "U_TENANTSCOPEDA");

    /** The tenant being activated. */
    private static final ExternalTenant TENANT_B = new ExternalTenant("tenant-scoped-b", "TENANTSCOPEDB", "U_TENANTSCOPEDB");

    /**
     * A tenant whose schema holds an incompatible table - its initialization fails, and only its own.
     */
    private static final ExternalTenant TENANT_C = new ExternalTenant("tenant-scoped-c", "TENANTSCOPEDC", "U_TENANTSCOPEDC");

    private static final String DB_PASSWORD = "tenant-scoped-init-it-password";

    private static final long INITIALIZATION_TIMEOUT_SECONDS = 300;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @BeforeAll
    static void enableTheApi() {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
    }

    @AfterAll
    static void disableTheApi() {
        Configuration.remove(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.getKey());
    }

    /**
     * Acceptance 1 of #7799: the rows of an initialized tenant - a deleted seed row included - are
     * unchanged by the activation of another tenant, which gets its own seed rows.
     */
    @Test
    @Order(1)
    void activatingATenantLeavesTheTenantsAlreadyInitializedUntouched() throws SQLException {
        publishSeededPerTenantTable();

        activateAndAwait(TENANT_A, "COMPLETED");
        assertEquals(Map.of(1, "Alpha", 2, "Beta"), rows(TENANT_A), "the seed rows of the first tenant");

        // what a tenant does to its own data: delete a seed row, add a row of its own
        execute(TENANT_A, "DELETE FROM " + qualifiedTable(TENANT_A) + " WHERE " + quoted(TENANT_A, "ORDER_ID") + " = 1");
        execute(TENANT_A, "INSERT INTO " + qualifiedTable(TENANT_A) + " (" + quoted(TENANT_A, "ORDER_ID") + ", "
                + quoted(TENANT_A, "ORDER_NAME") + ") VALUES (3, 'Gamma')");

        activateAndAwait(TENANT_B, "COMPLETED");

        assertEquals(Map.of(1, "Alpha", 2, "Beta"), rows(TENANT_B), "the activated tenant gets the seed rows");
        assertEquals(Map.of(2, "Beta", 3, "Gamma"), rows(TENANT_A),
                "the activation of another tenant must not re-import the seed row this tenant deleted");
    }

    /**
     * Acceptance 2 of #7799 / #7776: a tenant's status reflects that tenant only - a broken tenant does
     * not fail a healthy one, and a healthy one processed after it does not complete the broken one.
     */
    @Test
    @Order(2)
    void eachTenantReportsItsOwnOutcome() throws SQLException {
        prepareSchemaWithAnIncompatibleTable(TENANT_C);

        String failure = activateAndAwait(TENANT_C, "FAILED");
        assertTrue(failure.contains(PROJECT), "the failure must name the artefact that failed: " + failure);

        activateAndAwait(TENANT_B, "COMPLETED");

        assertEquals("FAILED", initializationStatus(TENANT_C), "a healthy tenant processed afterwards must not hide the failure");
        assertEquals("COMPLETED", initializationStatus(TENANT_A), "another tenant's failure is not this tenant's");
        assertEquals(Map.of(2, "Beta", 3, "Gamma"), rows(TENANT_A), "and re-activating a tenant leaves the others untouched");
    }

    /**
     * Activates a tenant whose database user and schema the "external provisioner" - this test -
     * creates first, and waits for its initialization to settle.
     *
     * @return the error the activation status reports, or null
     */
    private String activateAndAwait(ExternalTenant tenant, String expectedStatus) throws SQLException {
        createDatabaseUserAndSchema(tenant);
        restAssuredExecutor.execute(() -> {
            given().contentType(ContentType.JSON)
                   .body(Map.of("name", tenant.id()))
                   .when()
                   .put(TENANTS_PATH + tenant.id())
                   .then()
                   .statusCode(anyOf(is(200), is(201)));
            given().contentType(ContentType.JSON)
                   .body(Map.of("username", tenant.user(), "password", DB_PASSWORD, "schema", tenant.schema()))
                   .when()
                   .put(TENANTS_PATH + tenant.id() + "/datasources/default")
                   .then()
                   .statusCode(anyOf(is(200), is(201)));
            given().when()
                   .post(TENANTS_PATH + tenant.id() + "/activation")
                   .then()
                   .statusCode(202);
        });

        Awaitility.await()
                  .atMost(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .pollInterval(1, TimeUnit.SECONDS)
                  .until(() -> !"IN_PROGRESS".equals(initializationStatus(tenant)));
        assertEquals(expectedStatus, initializationStatus(tenant), "the initialization status of " + tenant.id());
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(TENANTS_PATH + tenant.id() + "/activation")
                                                                  .then()
                                                                  .extract()
                                                                  .path("error"));
    }

    private String initializationStatus(ExternalTenant tenant) {
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(TENANTS_PATH + tenant.id() + "/activation")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("status"));
    }

    /** A per-tenant table with two seed rows, published and reconciled for the tenants of today. */
    private void publishSeededPerTenantTable() {
        String table = """
                {
                    "name": "%s",
                    "type": "TABLE",
                    "columns": [
                        { "name": "ORDER_ID", "type": "INTEGER", "length": "0", "nullable": "false", "primaryKey": "true" },
                        { "name": "ORDER_NAME", "type": "VARCHAR", "length": "50", "nullable": "true", "primaryKey": "false" }
                    ]
                }
                """.formatted(TABLE);
        String csvim = """
                {
                    "files": [
                        {
                            "table": "%s",
                            "schema": "PUBLIC",
                            "file": "/%s/orders.csv",
                            "header": true,
                            "useHeaderNames": true,
                            "delimField": ",",
                            "delimEnclosing": "\\"",
                            "distinguishEmptyFromNull": true,
                            "version": "1.0"
                        }
                    ]
                }
                """.formatted(TABLE, PROJECT);
        String csv = "ORDER_ID,ORDER_NAME\n1,Alpha\n2,Beta\n";
        write(TABLE + ".table", table);
        write("orders.csv", csv);
        write("orders.csvim", csvim);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void write(String name, String content) {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + name,
                content.getBytes(StandardCharsets.UTF_8), false, "application/octet-stream", true);
    }

    /**
     * The table is already there, created by the provisioner with a key of another type than the
     * definition's: the platform refuses to change a column's type under its data, so the table fails -
     * in this tenant only.
     */
    private void prepareSchemaWithAnIncompatibleTable(ExternalTenant tenant) throws SQLException {
        createDatabaseUserAndSchema(tenant);
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        char escape = SqlDialectFactory.getDialect(defaultDataSource)
                                       .getEscapeSymbol();
        String qualified = escape + tenant.schema() + escape + "." + escape + TABLE + escape;
        try (Connection connection = defaultDataSource.getConnection()) {
            execute(connection, "CREATE TABLE " + qualified + " (" + escape + "ORDER_ID" + escape + " VARCHAR(10) NOT NULL PRIMARY KEY, "
                    + escape + "ORDER_NAME" + escape + " VARCHAR(50))");
        }
    }

    private boolean schemaExists(ExternalTenant tenant) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet schemas = connection.getMetaData()
                                              .getSchemas(null, tenant.schema())) {
            return schemas.next();
        }
    }

    /**
     * What the external provisioner does before it calls the API, as {@code TenantActivationIT} does.
     */
    private void createDatabaseUserAndSchema(ExternalTenant tenant) throws SQLException {
        if (schemaExists(tenant)) {
            return;
        }
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .user(tenant.user(), DB_PASSWORD)
                                          .build());
            execute(connection, SqlFactory.getNative(connection)
                                          .create()
                                          .schema(tenant.schema())
                                          .authorization(tenant.user())
                                          .build());
        }
    }

    private void execute(ExternalTenant tenant, String sql) throws SQLException {
        try (Connection connection = tenantDataSource(tenant).getConnection()) {
            execute(connection, sql);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    /** The rows of the tenant's table, read through the data source the API registered for it. */
    private Map<Integer, String> rows(ExternalTenant tenant) throws SQLException {
        Map<Integer, String> rows = new TreeMap<>();
        try (Connection connection = tenantDataSource(tenant).getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + quoted(tenant, "ORDER_ID") + ", " + quoted(tenant, "ORDER_NAME") + " FROM " + qualifiedTable(tenant));
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.put(resultSet.getInt(1), resultSet.getString(2));
            }
        }
        return rows;
    }

    private DirigibleDataSource tenantDataSource(ExternalTenant tenant) {
        return dataSourcesManager.getDataSource(tenant.id() + "_DefaultDB");
    }

    /**
     * Quoted with the dialect's own escape symbol: the platform creates its identifiers quoted, and an
     * unquoted name is folded by the database (down on PostgreSQL, up on H2).
     */
    private String qualifiedTable(ExternalTenant tenant) throws SQLException {
        return quoted(tenant, tenant.schema()) + "." + quoted(tenant, TABLE);
    }

    private String quoted(ExternalTenant tenant, String identifier) throws SQLException {
        char escape = SqlDialectFactory.getDialect(tenantDataSource(tenant))
                                       .getEscapeSymbol();
        return escape + identifier + escape;
    }

    /**
     * A tenant whose database user and schema exist outside the platform.
     *
     * @param id the tenant id
     * @param schema the schema the provisioner created
     * @param user the database user the provisioner created
     */
    private record ExternalTenant(String id, String schema, String user) {
    }
}
