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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

/**
 * Wires Liquibase against the {@code SystemDB} so the DIRIGIBLE_* platform schema is created and
 * versioned by JSON changelogs in {@code db/changelog/} rather than by Hibernate's
 * {@code hbm2ddl=update}. {@link LiquibaseEntityManagerFactoryDependsOnPostProcessor} adds a
 * {@code dependsOn("liquibaseSystemDB")} to every {@code EntityManagerFactory} bean at startup so
 * Hibernate sees the schema only after Liquibase has applied the changelogs. The dependency is
 * registered from this module — when {@code core-liquibase} is absent (e.g. slim unit-test
 * contexts), no dependency is declared and the EMF wires normally.
 *
 * <p>
 * {@link LegacyAwareSpringLiquibase} handles two non-pristine startup states that would otherwise
 * make Liquibase blow up trying to recreate tables that are still on disk:
 * <ul>
 * <li><b>Legacy production upgrade</b> — {@code DIRIGIBLE_*} tables exist from a previous
 * {@code hbm2ddl=update}-bootstrapped deployment, but no {@code DATABASECHANGELOG} ledger has ever
 * been written. The handler calls {@link Liquibase#changeLogSync} so every changeset is recorded as
 * applied without re-running its DDL, preserving the existing data.</li>
 * <li><b>Integration-test partial-cleanup state</b> — {@code DirigibleCleaner} between IT classes
 * issues H2 {@code DROP ALL OBJECTS}, which drops every schema object including
 * {@code DATABASECHANGELOG}. If a different connection holding stale entries races the drop, or if
 * a previous test left orphan tables on disk, the next test boots a fresh Liquibase against a DB
 * where the ledger exists but is empty while artefact tables are still present. The same
 * {@code changeLogSync} recovery applies. Hibernate's downstream {@code hbm2ddl=update} pass fills
 * in any genuinely-missing tables (in the rare case the orphans are incomplete).</li>
 * </ul>
 */
@Configuration
public class LiquibaseSystemConfig {

    private static final String CHANGELOG = "classpath:db/changelog/dirigible-system.json";

    /**
     * Sentinel table from the very first changeset — if it exists, the schema has been bootstrapped
     * before.
     */
    private static final String SENTINEL_TABLE = "DIRIGIBLE_SECURITY_ACCESS";

    /** Liquibase's tracking table. */
    private static final String DATABASECHANGELOG_TABLE = "DATABASECHANGELOG";

    @Bean
    public SpringLiquibase liquibaseSystemDB(@Qualifier("SystemDB") DataSource systemDB) {
        SpringLiquibase liquibase = new LegacyAwareSpringLiquibase();
        liquibase.setDataSource(systemDB);
        liquibase.setChangeLog(CHANGELOG);
        return liquibase;
    }

    /**
     * Dynamically adds {@code dependsOn("liquibaseSystemDB")} to every {@code EntityManagerFactory}
     * bean so Hibernate's startup runs strictly after Liquibase's changelog application. Modeled on
     * Spring Boot's own {@code LiquibaseAutoConfiguration} pattern — by living in
     * {@code core-liquibase} the BFPP is registered only when this module is on the classpath, so slim
     * unit-test contexts that omit it still boot.
     */
    @Configuration
    static class LiquibaseEntityManagerFactoryDependsOnPostProcessor extends EntityManagerFactoryDependsOnPostProcessor {
        LiquibaseEntityManagerFactoryDependsOnPostProcessor() {
            super("liquibaseSystemDB");
        }
    }

    static final class LegacyAwareSpringLiquibase extends SpringLiquibase {

        private static final Logger logger = LoggerFactory.getLogger(LegacyAwareSpringLiquibase.class);

        @Override
        protected void performUpdate(Liquibase liquibase) throws LiquibaseException {
            if (needsChangeLogSync()) {
                logger.info("Bootstrapping ledger via Liquibase changeLogSync(): sentinel table is already present and "
                        + "DATABASECHANGELOG is missing or empty. Every changeset is recorded as applied without re-running its DDL.");
                liquibase.changeLogSync(new Contexts(getContexts()), new LabelExpression(getLabelFilter()));
            }
            super.performUpdate(liquibase);
        }

        /**
         * Returns {@code true} when the SystemDB is in a state that requires marking every changeset as
         * applied without re-execution: either a legacy production upgrade (sentinel exists, no
         * {@code DATABASECHANGELOG}), or an integration-test partial-cleanup state (sentinel exists,
         * {@code DATABASECHANGELOG} exists but is empty). A fresh DB returns {@code false} and falls
         * through to the normal {@code update()} flow.
         */
        private boolean needsChangeLogSync() {
            try (Connection connection = getDataSource().getConnection()) {
                if (!tableExists(connection, SENTINEL_TABLE)) {
                    return false;
                }
                if (!tableExists(connection, DATABASECHANGELOG_TABLE)) {
                    return true;
                }
                return isEmpty(connection, DATABASECHANGELOG_TABLE);
            } catch (Exception e) {
                logger.warn("Could not probe SystemDB bootstrap state; proceeding with normal Liquibase update", e);
                return false;
            }
        }

        private boolean tableExists(Connection connection, String tableName) throws Exception {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String candidate : new String[] {tableName, tableName.toLowerCase()}) {
                try (ResultSet rs = metadata.getTables(null, null, candidate, new String[] {"TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isEmpty(Connection connection, String tableName) throws Exception {
            try (Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }
}
