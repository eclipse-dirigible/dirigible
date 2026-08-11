/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.core.liquibase;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import liquibase.integration.spring.SpringLiquibase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The system changelog must be idempotent against a database whose objects exist while the
 * {@code DATABASECHANGELOG} ledger does not record them. That state is real in two places: a legacy
 * {@code hbm2ddl}-bootstrapped deployment (whole-ledger recovery via the sentinel-triggered
 * {@code changeLogSync} in {@code LegacyAwareSpringLiquibase}), and the integration suite's
 * context-teardown race - {@code DirigibleCleaner}'s {@code DROP ALL OBJECTS} interleaves with
 * still-live contexts on the same file-backed H2, leaving SOME {@code DIRIGIBLE_*} tables present
 * with the ledger gone or empty. The sentinel heuristic cannot see that partial state (the sentinel
 * itself may be among the dropped tables), so every changeset carries its own
 * {@code preConditions onFail=MARK_RAN} guard: an object that already exists is marked ran instead
 * of failing the whole boot with "Table already exists" (the SecurityIT smoke flake).
 */
class SystemChangelogIdempotencyTest {

    /**
     * First run bootstraps a fresh H2. Then the ledger AND the sentinel are dropped while the business
     * tables stay - the exact partial state the integration-suite race produces (sentinel gone means
     * the whole-ledger {@code changeLogSync} recovery does NOT trigger, so only the per-changeset
     * guards can save the second update). The second run must boot cleanly: existing objects mark ran,
     * the dropped sentinel's own changesets re-run.
     */
    @Test
    void aPartiallyPresentSchemaWithoutALedgerBootsViaMarkRan() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:system-changelog-idempotency;DB_CLOSE_DELAY=-1");

        runChangelog(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE DATABASECHANGELOG");
            statement.execute("DROP TABLE DATABASECHANGELOGLOCK");
            statement.execute("DROP TABLE DIRIGIBLE_SECURITY_ACCESS CASCADE");
        }

        runChangelog(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            // The surviving tables were marked ran, not re-created ...
            try (ResultSet rs =
                    statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DIRIGIBLE_BPMN'")) {
                rs.next();
                assertTrue(rs.getInt(1) == 1, "DIRIGIBLE_BPMN must still exist after the second update");
            }
            // ... and the dropped sentinel's own changeset re-ran (its guard found nothing to skip).
            try (ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'DIRIGIBLE_SECURITY_ACCESS'")) {
                rs.next();
                assertTrue(rs.getInt(1) == 1, "the dropped sentinel table must be re-created by its own changeset");
            }
        }
    }

    private void runChangelog(DataSource dataSource) throws Exception {
        SpringLiquibase liquibase = new LiquibaseSystemConfig().liquibaseSystemDB(dataSource);
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.afterPropertiesSet();
    }
}
