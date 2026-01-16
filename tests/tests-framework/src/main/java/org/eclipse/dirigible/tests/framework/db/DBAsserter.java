/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.db;

import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class DBAsserter {
    private final DataSourcesManager dataSourcesManager;

    protected DBAsserter(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    public void assertRowCount(String tableName, int expectedRowCount) {
        Table table = getDefaultDbTable(tableName);

        Assertions.assertThat(table)
                  .hasNumberOfRows(expectedRowCount);
    }

    public Table getDefaultDbTable(String tableName) {
        String schemaName = getDefaultDBDefaultSchema();
        return getDefaultDbTable(schemaName, tableName);
    }

    private String getDefaultDBDefaultSchema() {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();

        if (dataSource.isOfType(DatabaseSystem.H2)) {
            return "PUBLIC";
        }

        if (dataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
            return "public";
        }

        if (dataSource.isOfType(DatabaseSystem.MSSQL)) {
            return "dbo";
        }

        throw new IllegalStateException("Missing default schema for datasource " + dataSource);
    }

    public Table getDefaultDbTable(String schemaName, String tableName) {
        AssertDbConnection connection = getDefaultDbAssertDbConnection();
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
        ISqlDialect dialect = getDefaultDbDialect(dataSource);

        char escapeSymbol = dialect.getEscapeSymbol();
        String escapedTableName = escapeSymbol + tableName + escapeSymbol;

        String escapedSchemaName = escapeSymbol + schemaName + escapeSymbol;

        String fullTableName = escapedSchemaName + "." + escapedTableName;
        return connection.table(fullTableName)
                         .build();
    }

    private ISqlDialect getDefaultDbDialect(DataSource dataSource) {
        try {
            return SqlDialectFactory.getDialect(dataSource);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create dialect for " + dataSource, e);
        }
    }

    public AssertDbConnection getDefaultDbAssertDbConnection() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        return AssertDbConnectionFactory.of(dataSource)
                                        .create();
    }

    public void assertRowHasColumnWithValue(String tableName, int rowIndex, String columnName, Object expectedValue) {
        Table table = getDefaultDbTable(tableName);

        Assertions.assertThat(table)
                  .row(rowIndex)
                  .value(columnName)
                  .isEqualTo(expectedValue);
    }

    public void assertTableHasColumn(String tableName, String columnName) {
        Table table = getDefaultDbTable(tableName);

        Assertions.assertThat(table)
                  .column(columnName);
    }

}

