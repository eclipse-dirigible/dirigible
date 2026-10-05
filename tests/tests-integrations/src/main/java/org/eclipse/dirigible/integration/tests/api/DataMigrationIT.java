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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A {@code .migration} applies exactly once per tenant schema, after the table it changes is
 * synchronized, recorded in that schema's {@code DIRIGIBLE_MIGRATIONS} (#7636):
 * <ol>
 * <li>a project's table and two migrations land in the default tenant and a provisioned tenant, the
 * second migration (a backfill) after the first (the rows it backfills);</li>
 * <li>every multitenant artefact is synchronized again - what provisioning a new tenant does, and
 * the same re-parse a boot against a fresh system database runs - and nothing applies twice, while
 * the new tenant is migrated;</li>
 * <li>an applied file edited afterwards is a FAILED artefact - counted by the health endpoint and
 * keeping the boot from being clean - and restoring it heals it;</li>
 * <li>the ledger endpoint lists what applied.</li>
 * </ol>
 * Pure HTTP / JDBC - no Selenide, no IDE.
 */
class DataMigrationIT extends IntegrationTest {

    private static final String PROJECT = "data-migration-it";

    private static final String TABLE_PATH = registryPath("/tables/order.table");
    private static final String SEED_PATH = registryPath("/migrations/V1__seed_orders.migration");
    private static final String BACKFILL_LOCATION = "/" + PROJECT + "/migrations/V2__backfill_status.migration";
    private static final String BACKFILL_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + BACKFILL_LOCATION;

    private static final String TABLE_SOURCE = """
            {
                "name": "DATA_MIGRATION_IT_ORDER",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "nullable": false,
                        "name": "ORDER_ID"
                    },
                    {
                        "type": "VARCHAR",
                        "length": 20,
                        "nullable": true,
                        "name": "ORDER_STATUS"
                    }
                ]
            }
            """;

    private static final String SEED_SOURCE = """
            -- The rows a later migration backfills.
            INSERT INTO "DATA_MIGRATION_IT_ORDER" ("ORDER_ID", "ORDER_STATUS") VALUES (1, NULL);
            INSERT INTO "DATA_MIGRATION_IT_ORDER" ("ORDER_ID", "ORDER_STATUS") VALUES (2, 'CLOSED');
            INSERT INTO "DATA_MIGRATION_IT_ORDER" ("ORDER_ID", "ORDER_STATUS") VALUES (3, NULL);
            """;

    private static final String BACKFILL_SOURCE = """
            -- tenant: each
            UPDATE "DATA_MIGRATION_IT_ORDER" SET "ORDER_STATUS" = 'OPEN' WHERE "ORDER_STATUS" IS NULL;
            """;

    private static final String BACKFILL_EDITED = """
            -- tenant: each
            UPDATE "DATA_MIGRATION_IT_ORDER" SET "ORDER_STATUS" = 'NEW' WHERE "ORDER_STATUS" IS NULL;
            """;

    private static final long TIMEOUT_SECONDS = 120;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void aMigrationAppliesOncePerTenantAndAnEditAfterwardsFailsIt() throws Exception {
        DirigibleTestTenant tenant = new DirigibleTestTenant(PROJECT);
        createTenants(tenant);
        waitForTenantProvisioning(tenant);

        write(TABLE_PATH, TABLE_SOURCE);
        write(SEED_PATH, SEED_SOURCE);
        write(BACKFILL_PATH, BACKFILL_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        assertThat(defaultTenant(this::ledgerVersions)).containsExactlyInAnyOrder("1", "2");
        assertThat(defaultTenant(this::statuses)).containsExactly("OPEN", "CLOSED", "OPEN");
        assertThat(inTenant(tenant, this::ledgerVersions)).containsExactlyInAnyOrder("1", "2");
        assertThat(inTenant(tenant, this::statuses)).containsExactly("OPEN", "CLOSED", "OPEN");

        // A row the backfill would rewrite if it ran again.
        defaultTenant(() -> execute("UPDATE \"DATA_MIGRATION_IT_ORDER\" SET \"ORDER_STATUS\" = NULL WHERE \"ORDER_ID\" = 3"));
        inTenant(tenant, () -> execute("UPDATE \"DATA_MIGRATION_IT_ORDER\" SET \"ORDER_STATUS\" = NULL WHERE \"ORDER_ID\" = 3"));

        // Provisioning a tenant re-synchronizes every multitenant artefact in every tenant.
        DirigibleTestTenant newTenant = new DirigibleTestTenant(PROJECT + "-late");
        createTenants(newTenant);
        waitForTenantProvisioning(newTenant);
        Awaitility.await()
                  .pollInterval(2, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> inTenant(newTenant, this::ledgerVersions).size() == 2);
        synchronizationProcessor.forceProcessSynchronizers();

        assertThat(inTenant(newTenant, this::statuses)).containsExactly("OPEN", "CLOSED", "OPEN");
        assertThat(defaultTenant(this::ledgerVersions)).as("one ledger row per migration, however often it was synchronized")
                                                       .containsExactlyInAnyOrder("1", "2");
        assertThat(inTenant(tenant, this::ledgerVersions)).containsExactlyInAnyOrder("1", "2");
        assertThat(defaultTenant(this::statuses)).as("an applied migration never runs again")
                                                 .containsExactly("OPEN", "CLOSED", null);
        assertThat(inTenant(tenant, this::statuses)).containsExactly("OPEN", "CLOSED", null);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/migrations")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.project == '" + PROJECT + "' }.version", containsInAnyOrder("1", "2"))
                                                 .body("find { it.version == '2' && it.project == '" + PROJECT + "' }.location",
                                                         equalTo(BACKFILL_LOCATION)),
                TIMEOUT_SECONDS);

        // An applied migration edited afterwards: FAILED, and nothing runs.
        write(BACKFILL_PATH, BACKFILL_EDITED);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/actuator/health")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.migrations.status", equalTo("UP"))
                                                 .body("components.migrations.details.failed", equalTo(1))
                                                 .body("components.migrations.details.pending", equalTo(0))
                                                 .body("components.artefacts.details.failedByType.migration", equalTo(1)),
                TIMEOUT_SECONDS);
        assertThat(PlatformReadiness.getInstance()
                                    .isCleanBoot()).as("DIRIGIBLE_READINESS_REQUIRE_CLEAN_BOOT withholds readiness on a failed migration")
                                                   .isFalse();
        assertThat(defaultTenant(this::statuses)).containsExactly("OPEN", "CLOSED", null);

        // Restoring the file heals it - the ledger already has it.
        write(BACKFILL_PATH, BACKFILL_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/actuator/health")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.migrations.details.failed", equalTo(0))
                                                 .body("components.artefacts.details.failedByType", not(hasKey("migration"))),
                TIMEOUT_SECONDS);
        assertThat(defaultTenant(this::statuses)).containsExactly("OPEN", "CLOSED", null);
    }

    private List<String> ledgerVersions() throws SQLException {
        return column("SELECT \"MIGRATION_VERSION\" FROM \"DIRIGIBLE_MIGRATIONS\" WHERE \"MIGRATION_PROJECT\" = ?", PROJECT);
    }

    private List<String> statuses() throws SQLException {
        return column("SELECT \"ORDER_STATUS\" FROM \"DATA_MIGRATION_IT_ORDER\" ORDER BY \"ORDER_ID\"");
    }

    private List<String> column(String sql, String... parameters) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            List<String> values = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
            return values;
        }
    }

    private Void execute(String sql) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
        return null;
    }

    /** Outside a tenant scope the default datasource is the default tenant's database. */
    private static <T> T defaultTenant(SqlCall<T> call) throws SQLException {
        return call.call();
    }

    private <T> T inTenant(DirigibleTestTenant tenant, SqlCall<T> call) throws SQLException {
        return tenantContext.execute(tenant.getId(), call::call);
    }

    private void write(String path, String source) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
    }

    private static String registryPath(String path) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + path;
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T call() throws SQLException;
    }
}
