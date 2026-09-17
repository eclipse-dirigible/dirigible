/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.config;

import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DatabaseSystemDeterminer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the Quartz JDBC job store driver delegate for the database the job store actually runs
 * on.
 *
 * <p>
 * The standard delegate reads the trigger and job data columns as JDBC blobs, which PostgreSQL
 * answers with a {@code BYTEA} the driver refuses to convert - every trigger read then fails and no
 * job ever fires. The delegate is therefore derived from the job store's own data source, with
 * {@code DIRIGIBLE_SCHEDULER_DATABASE_DELEGATE} kept as an explicit override.
 */
final class QuartzDriverDelegateResolver {

    /** The delegate every database not needing a specialized one uses. */
    static final String STANDARD_DELEGATE = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";

    /** The configuration key overriding the derived delegate. */
    static final String DELEGATE_OVERRIDE_KEY = "DIRIGIBLE_SCHEDULER_DATABASE_DELEGATE";

    /** The delegate PostgreSQL needs - named in the failure message as the fix to apply. */
    static final String POSTGRESQL_DELEGATE = "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzDriverDelegateResolver.class);

    private static final Map<DatabaseSystem, String> DELEGATES_BY_DATABASE = Map.of(//
            DatabaseSystem.POSTGRESQL, POSTGRESQL_DELEGATE, //
            DatabaseSystem.MSSQL, "org.quartz.impl.jdbcjobstore.MSSQLDelegate"//
    );

    private QuartzDriverDelegateResolver() {}

    /**
     * Resolves the delegate for the given job store data source - the configured override when set,
     * otherwise the delegate of the data source's database.
     *
     * @param dataSource the job store data source
     * @return the fully qualified delegate class name, never blank
     */
    static String resolve(DataSource dataSource) {
        String configuredDelegate = Configuration.get(DELEGATE_OVERRIDE_KEY);
        if (StringUtils.isNotBlank(configuredDelegate)) {
            LOGGER.info("Using the configured Quartz driver delegate [{}] from [{}]", configuredDelegate, DELEGATE_OVERRIDE_KEY);
            return configuredDelegate.trim();
        }

        DatabaseSystem databaseSystem = determineDatabaseSystem(dataSource);
        String delegate = delegateFor(databaseSystem);
        LOGGER.info("Using the Quartz driver delegate [{}] derived from database [{}]", delegate, databaseSystem);
        return delegate;
    }

    /**
     * The delegate the given database needs.
     *
     * @param databaseSystem the database the job store runs on
     * @return the fully qualified delegate class name
     */
    static String delegateFor(DatabaseSystem databaseSystem) {
        return DELEGATES_BY_DATABASE.getOrDefault(databaseSystem, STANDARD_DELEGATE);
    }

    private static DatabaseSystem determineDatabaseSystem(DataSource dataSource) {
        try {
            return DatabaseSystemDeterminer.determine(dataSource);
        } catch (SQLException ex) {
            LOGGER.warn(
                    "Failed to determine the database of the scheduler data source. Falling back to [{}]."
                            + " Set [{}] explicitly if the scheduler cannot read its triggers",
                    STANDARD_DELEGATE, DELEGATE_OVERRIDE_KEY, ex);
            return DatabaseSystem.UNKNOWN;
        }
    }
}
