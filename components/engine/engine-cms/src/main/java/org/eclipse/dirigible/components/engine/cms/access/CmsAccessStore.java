/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Persistence for the CMS path grants.
 * <p>
 * Every operation runs against the default datasource, which inside a tenant execution scope is
 * routed to that tenant's schema - so a distinct {@code DIRIGIBLE_CMS_ACCESS} table lives in every
 * tenant schema and a grant can never leak between tenants. The table is created on first use.
 * <p>
 * This is deliberately raw SQL rather than a JPA entity: JPA entities resolve against the default
 * schema and are not tenant-routed, which is why the per-tenant configuration and document-number
 * stores are written the same way.
 */
@Component
class CmsAccessStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmsAccessStore.class);

    /** Unquoted - for metadata checks and for the auto-quoting statement builders. */
    private static final String TABLE_NAME = "DIRIGIBLE_CMS_ACCESS";

    private static final String COLUMN_PATH = "CMS_ACCESS_PATH";
    private static final String COLUMN_METHOD = "CMS_ACCESS_METHOD";
    private static final String COLUMN_ROLE = "CMS_ACCESS_ROLE";
    private static final String COLUMN_CREATED_BY = "CMS_ACCESS_CREATED_BY";
    private static final String COLUMN_CREATED_AT = "CMS_ACCESS_CREATED_AT";

    /** Quoted - the DDL builder takes pre-quoted identifiers. */
    private static final String QUOTED_TABLE = "\"" + TABLE_NAME + "\"";
    private static final String QUOTED_PATH = "\"" + COLUMN_PATH + "\"";
    private static final String QUOTED_METHOD = "\"" + COLUMN_METHOD + "\"";
    private static final String QUOTED_ROLE = "\"" + COLUMN_ROLE + "\"";
    private static final String QUOTED_CREATED_BY = "\"" + COLUMN_CREATED_BY + "\"";
    private static final String QUOTED_CREATED_AT = "\"" + COLUMN_CREATED_AT + "\"";

    private final DataSourcesManager dataSourcesManager;

    CmsAccessStore(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * Every grant of the current tenant.
     *
     * @return the grants, never null
     * @throws SQLException when the read fails
     */
    List<CmsAccessGrant> readAll() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .build();
            List<CmsAccessGrant> grants = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    grants.add(new CmsAccessGrant(resultSet.getString(COLUMN_PATH), resultSet.getString(COLUMN_METHOD),
                            resultSet.getString(COLUMN_ROLE)));
                }
            }
            return grants;
        }
    }

    /**
     * Adds a grant, ignoring a duplicate.
     *
     * @param grant the grant
     * @param user who granted it
     * @throws SQLException when the write fails
     */
    void add(CmsAccessGrant grant, String user) throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            if (exists(connection, grant)) {
                return;
            }
            String sql = SqlFactory.getNative(connection)
                                   .insert()
                                   .into(TABLE_NAME)
                                   .column(COLUMN_PATH)
                                   .column(COLUMN_METHOD)
                                   .column(COLUMN_ROLE)
                                   .column(COLUMN_CREATED_BY)
                                   .column(COLUMN_CREATED_AT)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, grant.path());
                statement.setString(2, grant.method());
                statement.setString(3, grant.role());
                statement.setString(4, user);
                statement.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                statement.executeUpdate();
            }
        }
    }

    /**
     * Removes a grant.
     *
     * @param grant the grant
     * @throws SQLException when the write fails
     */
    void remove(CmsAccessGrant grant) throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .delete()
                                   .from(TABLE_NAME)
                                   .where(COLUMN_PATH + " = ?")
                                   .where(COLUMN_METHOD + " = ?")
                                   .where(COLUMN_ROLE + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, grant.path());
                statement.setString(2, grant.method());
                statement.setString(3, grant.role());
                statement.executeUpdate();
            }
        }
    }

    private boolean exists(Connection connection, CmsAccessGrant grant) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column("*")
                               .from(TABLE_NAME)
                               .where(COLUMN_PATH + " = ?")
                               .where(COLUMN_METHOD + " = ?")
                               .where(COLUMN_ROLE + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, grant.path());
            statement.setString(2, grant.method());
            statement.setString(3, grant.role());
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
                               .column(QUOTED_PATH, DataType.VARCHAR, false, false, false, "(1024)")
                               .column(QUOTED_METHOD, DataType.VARCHAR, false, false, false, "(20)")
                               .column(QUOTED_ROLE, DataType.VARCHAR, false, false, false, "(255)")
                               .column(QUOTED_CREATED_BY, DataType.VARCHAR, false, true, false, "(255)")
                               .column(QUOTED_CREATED_AT, DataType.TIMESTAMP, false, true, false)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created the per-tenant CMS access table using sql [{}]", sql);
        } catch (SQLException e) {
            // A concurrent request may have created it in the meantime; tolerate that.
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, TABLE_NAME)) {
                LOGGER.debug("The per-tenant CMS access table already exists after a concurrent creation.", e);
                return;
            }
            throw e;
        }
    }
}
