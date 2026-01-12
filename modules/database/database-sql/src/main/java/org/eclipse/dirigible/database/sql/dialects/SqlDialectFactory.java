/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DatabaseSystemDeterminer;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.ISqlDialectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating SqlDialect objects.
 */
public class SqlDialectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDialectFactory.class);

    /** The Constant ACCESS_MANAGERS. */
    private static final ServiceLoader<ISqlDialectProvider> SQL_PROVIDERS = ServiceLoader.load(ISqlDialectProvider.class);

    private static final Map<DatabaseSystem, ISqlDialect> dialectsBySystem = Collections.synchronizedMap(new HashMap<>());

    static {
        loadDefaultDialectsBySystem();
    }

    public static ISqlDialect getDialect(DataSource dataSource) throws SQLException {
        if (dataSource instanceof DirigibleDataSource dds) {
            return getDialect(dds);
        }

        try (Connection connection = dataSource.getConnection()) {
            return getDialect(connection);
        }
    }

    /**
     * Gets the dialect.
     *
     * @param connection the connection
     * @return the dialect
     * @throws SQLException the SQL exception
     */
    public static ISqlDialect getDialect(Connection connection) throws SQLException {
        if (connection instanceof DirigibleConnection dc) {
            return getDialect(dc);
        }
        DatabaseMetaData metaData = connection.getMetaData();
        String jdbcUrl = metaData.getURL();
        String driver = metaData.getDriverName();
        DatabaseSystem databaseSystem = DatabaseSystemDeterminer.determine(jdbcUrl, driver);

        return getDialect(databaseSystem);
    }

    public static ISqlDialect getDialect(DirigibleConnection connection) throws SQLException {
        return getDialect(connection.getDatabaseSystem());
    }

    public static ISqlDialect getDialect(DirigibleDataSource dataSource) throws SQLException {
        DatabaseSystem databaseSystem = dataSource.getDatabaseSystem();
        return getDialect(databaseSystem);
    }

    public static ISqlDialect getDialect(DatabaseSystem databaseSystem) {
        ISqlDialect dialect = dialectsBySystem.get(databaseSystem);
        if (dialect == null) {
            loadDefaultDialectsBySystem();
            dialect = dialectsBySystem.get(databaseSystem);
            if (dialect == null) {
                throw new IllegalStateException("Database dialect for [" + databaseSystem + "] is not available.");
            }
        }
        LOGGER.debug("Loaded dialect [{}] for [{}]", dialect, databaseSystem);
        return dialect;
    }

    private static void loadDefaultDialectsBySystem() {
        for (ISqlDialectProvider provider : SQL_PROVIDERS) {
            dialectsBySystem.put(provider.getDatabaseSystem(), provider.getDialect());
        }
    }

}
