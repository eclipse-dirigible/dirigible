/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persistence for the per-tenant configuration entries. All operations run against the default
 * datasource, which - inside a tenant execution scope - is automatically routed to the current
 * tenant's schema, so a distinct DIRIGIBLE_CONFIGURATIONS table lives in every tenant schema. The
 * table is created on first use (create-if-absent).
 */
@Component
class TenantConfigurationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigurationStore.class);

    /** Unquoted table name - used for metadata existence checks. */
    private static final String TABLE_NAME = "DIRIGIBLE_CONFIGURATIONS";

    private static final String COLUMN_KEY = "CONFIGURATION_KEY";

    private static final String COLUMN_VALUE = "CONFIGURATION_VALUE";

    /** Quoted identifiers - used for DDL so the identifiers stay case-exact across dialects. */
    private static final String QUOTED_TABLE = "\"" + TABLE_NAME + "\"";

    private static final String QUOTED_KEY = "\"" + COLUMN_KEY + "\"";

    private static final String QUOTED_VALUE = "\"" + COLUMN_VALUE + "\"";

    private final DataSourcesManager dataSourcesManager;

    TenantConfigurationStore(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * Reads all configuration entries of the current tenant.
     *
     * @return the entries keyed by configuration key (insertion-ordered), never {@code null}
     * @throws SQLException if the read fails
     */
    Map<String, String> readAll() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);

            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .build();
            Map<String, String> result = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getString(COLUMN_KEY), resultSet.getString(COLUMN_VALUE));
                }
            }
            return result;
        }
    }

    /**
     * Inserts or updates a configuration entry of the current tenant.
     *
     * @param key the configuration key
     * @param value the configuration value
     * @throws SQLException if the write fails
     */
    void set(String key, String value) throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);

            if (exists(connection, key)) {
                String sql = SqlFactory.getNative(connection)
                                       .update()
                                       .table(TABLE_NAME)
                                       .set(COLUMN_VALUE, "?")
                                       .where(COLUMN_KEY + " = ?")
                                       .build();
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, value);
                    statement.setString(2, key);
                    statement.executeUpdate();
                }
            } else {
                String sql = SqlFactory.getNative(connection)
                                       .insert()
                                       .into(TABLE_NAME)
                                       .column(COLUMN_KEY)
                                       .column(COLUMN_VALUE)
                                       .build();
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, key);
                    statement.setString(2, value);
                    statement.executeUpdate();
                }
            }
        }
    }

    /**
     * Deletes a configuration entry of the current tenant.
     *
     * @param key the configuration key
     * @throws SQLException if the delete fails
     */
    void delete(String key) throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);

            String sql = SqlFactory.getNative(connection)
                                   .delete()
                                   .from(TABLE_NAME)
                                   .where(COLUMN_KEY + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                statement.executeUpdate();
            }
        }
    }

    private boolean exists(Connection connection, String key) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column("*")
                               .from(TABLE_NAME)
                               .where(COLUMN_KEY + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void ensureTableExists(Connection connection) throws SQLException {
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, TABLE_NAME)) {
            return;
        }
        String sql = SqlFactory.getNative(connection)
                               .create()
                               .table(QUOTED_TABLE)
                               .column(QUOTED_KEY, DataType.VARCHAR, true, false, false, "(255)")
                               .column(QUOTED_VALUE, DataType.VARCHAR, false, true, false, "(4000)")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created per-tenant configuration table using sql [{}]", sql);
        } catch (SQLException ex) {
            // Another concurrent request may have created the table in the meantime; tolerate it.
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, TABLE_NAME)) {
                LOGGER.debug("Per-tenant configuration table already exists after a concurrent creation.", ex);
                return;
            }
            throw ex;
        }
    }

}
