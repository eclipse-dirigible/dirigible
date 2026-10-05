/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Outcome;
import org.eclipse.dirigible.components.data.migrations.MigrationExecutor.Status;
import org.eclipse.dirigible.components.data.migrations.MigrationLedger.Entry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The executor on a real in-memory database: the transaction and the ledger are what it is about.
 */
class MigrationExecutorTest {

    private static final String TENANT = "tenant-a";

    private static final String V1 = "/orders/migrations/001__backfill_status.migration";
    private static final String V2 = "/orders/migrations/002__close_old.migration";

    private final MigrationLedger ledger = new MigrationLedger();
    private final MigrationExecutor executor = new MigrationExecutor(ledger);

    private JdbcDataSource dataSource;

    @BeforeEach
    void createTheTable() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:migration_executor_test;DB_CLOSE_DELAY=-1");
        execute("CREATE TABLE ORDERS (ID INT PRIMARY KEY, STATUS VARCHAR(20))");
        execute("INSERT INTO ORDERS VALUES (1, NULL), (2, 'CLOSED')");
    }

    @AfterEach
    void dropEverything() throws SQLException {
        execute("DROP ALL OBJECTS");
    }

    @Test
    void aMigrationRunsAndIsRecordedOnce() throws Exception {
        MigrationScript script = script(V1, "UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;");

        assertThat(executor.apply(script, V1, TENANT, dataSource, List.of())
                           .status()).isEqualTo(Status.APPLIED);
        assertThat(status(1)).isEqualTo("OPEN");

        // What a second boot, a new tenant's re-trigger or a retry does: nothing.
        execute("UPDATE ORDERS SET STATUS = NULL WHERE ID = 1");
        assertThat(executor.apply(script, V1, TENANT, dataSource, List.of())
                           .status()).isEqualTo(Status.ALREADY_APPLIED);
        assertThat(status(1)).isNull();

        List<Entry> entries = ledger.findAll(dataSource);
        assertThat(entries).singleElement()
                           .satisfies(entry -> {
                               assertThat(entry.project()).isEqualTo("orders");
                               assertThat(entry.version()).isEqualTo("001");
                               assertThat(entry.location()).isEqualTo(V1);
                               assertThat(entry.checksum()).isEqualTo(script.checksum());
                               assertThat(entry.tenant()).isEqualTo(TENANT);
                               assertThat(entry.appliedAt()).isNotNull();
                           });
    }

    @Test
    void anAppliedMigrationEditedAfterwardsFailsAndDoesNotRun() throws Exception {
        executor.apply(script(V1, "UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;"), V1, TENANT, dataSource, List.of());
        execute("UPDATE ORDERS SET STATUS = NULL WHERE ID = 1");

        Outcome outcome =
                executor.apply(script(V1, "UPDATE ORDERS SET STATUS = 'NEW' WHERE STATUS IS NULL;"), V1, TENANT, dataSource, List.of());

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        assertThat(outcome.message()).contains(V1, "never re-run", "idempotent: true");
        assertThat(status(1)).isNull();
    }

    @Test
    void anIdempotentMigrationEditedAfterwardsRunsAgainAndIsRecordedWithItsNewContent() throws Exception {
        executor.apply(script(V1, "-- idempotent: true\nUPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;"), V1, TENANT, dataSource,
                List.of());
        execute("UPDATE ORDERS SET STATUS = NULL WHERE ID = 1");
        MigrationScript edited = script(V1, "-- idempotent: true\nUPDATE ORDERS SET STATUS = 'NEW' WHERE STATUS IS NULL;");

        assertThat(executor.apply(edited, V1, TENANT, dataSource, List.of())
                           .status()).isEqualTo(Status.APPLIED);

        assertThat(status(1)).isEqualTo("NEW");
        assertThat(ledger.findAll(dataSource)).singleElement()
                                              .extracting(Entry::checksum)
                                              .isEqualTo(edited.checksum());
    }

    @Test
    void aFailingStatementRollsBackTheWholeMigrationAndRecordsNothing() throws Exception {
        MigrationScript script = script(V1, """
                UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;
                UPDATE NO_SUCH_TABLE SET STATUS = 'OPEN';
                """);

        Outcome outcome = executor.apply(script, V1, TENANT, dataSource, List.of());

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        assertThat(outcome.message()).contains(V1, TENANT, "NO_SUCH_TABLE");
        assertThat(outcome.cause()).isNotNull();
        assertThat(status(1)).as("the first statement is rolled back with the second")
                             .isNull();
        assertThat(ledger.findAll(dataSource)).isEmpty();
    }

    @Test
    void aLaterVersionWaitsForAnEarlierOneThatHasNotAppliedYet() throws Exception {
        MigrationScript first = script(V1, "UPDATE ORDERS SET STATUS = 'OPEN' WHERE STATUS IS NULL;");
        MigrationScript second = script(V2, "UPDATE ORDERS SET STATUS = 'ARCHIVED' WHERE STATUS = 'OPEN';");
        Migration pendingFirst = artefact(V1, first, ArtefactLifecycle.NEW);

        Outcome waiting = executor.apply(second, V2, TENANT, dataSource, List.of(pendingFirst));

        assertThat(waiting.status()).isEqualTo(Status.WAITING);
        assertThat(waiting.message()).contains("waits for version [001]");
        assertThat(status(1)).isNull();

        executor.apply(first, V1, TENANT, dataSource, List.of());
        assertThat(executor.apply(second, V2, TENANT, dataSource, List.of(pendingFirst))
                           .status()).isEqualTo(Status.APPLIED);
        assertThat(status(1)).isEqualTo("ARCHIVED");
    }

    @Test
    void aLaterVersionFailsWhenTheEarlierOneFailed() throws Exception {
        MigrationScript first = script(V1, "UPDATE NO_SUCH_TABLE SET STATUS = 'OPEN';");
        MigrationScript second = script(V2, "UPDATE ORDERS SET STATUS = 'ARCHIVED';");

        Outcome outcome = executor.apply(second, V2, TENANT, dataSource, List.of(artefact(V1, first, ArtefactLifecycle.FAILED)));

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        assertThat(outcome.message()).contains("waits for version [001]", "which failed");
        assertThat(status(2)).isEqualTo("CLOSED");
    }

    private static MigrationScript script(String location, String sql) throws ParseException {
        return MigrationScript.parse(location, sql.getBytes(StandardCharsets.UTF_8));
    }

    private static Migration artefact(String location, MigrationScript script, ArtefactLifecycle lifecycle) {
        Migration migration = new Migration(location, location.substring(location.lastIndexOf('/') + 1), script);
        migration.setLifecycle(lifecycle);
        return migration;
    }

    private String status(int id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT STATUS FROM ORDERS WHERE ID = " + id)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
