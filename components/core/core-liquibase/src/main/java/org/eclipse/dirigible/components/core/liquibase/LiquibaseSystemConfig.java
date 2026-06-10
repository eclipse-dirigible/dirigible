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
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * {@code hbm2ddl=update}. The {@code entityManagerFactory} bean in {@code DataSourceSystemConfig}
 * must {@code @DependsOn("liquibaseSystemDB")} so Hibernate's (validate-only) startup runs after
 * the changelogs have been applied.
 *
 * <p>
 * Legacy deployments that were originally bootstrapped via {@code hbm2ddl=update} already have
 * every system table but no {@code DATABASECHANGELOG} entries. {@link #liquibaseSystemDB} extends
 * {@link SpringLiquibase} to detect that case and call
 * {@link Liquibase#changeLogSync(Contexts, LabelExpression)} first — every changeset is recorded as
 * applied without re-running its DDL, preserving the existing data. Fresh deployments fall through
 * to the normal {@code update()} flow and create the schema from scratch.
 */
@Configuration
public class LiquibaseSystemConfig {

    private static final String CHANGELOG = "classpath:db/changelog/dirigible-system.json";

    /** Sentinel table from the very first changeset — if it exists, the schema is already populated. */
    private static final String SENTINEL_TABLE = "DIRIGIBLE_SECURITY_ACCESS";

    /** Liquibase's tracking table. Absence indicates the schema was bootstrapped some other way. */
    private static final String DATABASECHANGELOG_TABLE = "DATABASECHANGELOG";

    @Bean
    public SpringLiquibase liquibaseSystemDB(@Qualifier("SystemDB") DataSource systemDB) {
        SpringLiquibase liquibase = new LegacyAwareSpringLiquibase();
        liquibase.setDataSource(systemDB);
        liquibase.setChangeLog(CHANGELOG);
        return liquibase;
    }

    static final class LegacyAwareSpringLiquibase extends SpringLiquibase {

        private static final Logger logger = LoggerFactory.getLogger(LegacyAwareSpringLiquibase.class);

        @Override
        protected void performUpdate(Liquibase liquibase) throws LiquibaseException {
            if (isLegacyDeployment()) {
                logger.info("Legacy deployment detected — DIRIGIBLE_* tables already exist but DATABASECHANGELOG is empty; "
                        + "marking every changeset as applied via changelogSync() to preserve existing data.");
                liquibase.changeLogSync(new Contexts(getContexts()), new LabelExpression(getLabelFilter()));
            }
            super.performUpdate(liquibase);
        }

        private boolean isLegacyDeployment() {
            try (Connection connection = getDataSource().getConnection()) {
                return tableExists(connection, SENTINEL_TABLE) && !tableExists(connection, DATABASECHANGELOG_TABLE);
            } catch (Exception e) {
                logger.warn("Could not probe for legacy deployment state; proceeding with normal Liquibase update", e);
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
    }
}
