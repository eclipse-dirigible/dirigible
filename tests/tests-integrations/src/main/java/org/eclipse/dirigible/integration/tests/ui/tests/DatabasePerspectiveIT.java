/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.db.DBAsserter;
import org.eclipse.dirigible.tests.framework.ide.DatabasePerspective;
import org.eclipse.dirigible.tests.framework.util.SleepUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

public class DatabasePerspectiveIT extends UserInterfaceIntegrationTest {

    private static final String TEST_TABLE_NAME = "STUDENT";

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DBAsserter dbAsserter;

    @Test
    void testDatabaseFunctionality() throws SQLException {
        DatabasePerspective databasePerspective = ide.openDatabasePerspective();

        createTestTable(databasePerspective);

        String schema = getSchema();
        expandSubviews(schema, databasePerspective);
        assertAvailabilityOfSubitems(databasePerspective);

        databasePerspective.assertEmptyTable(TEST_TABLE_NAME);

        insertTestRecord(databasePerspective);
        assertInsertedRecord(databasePerspective);
    }

    private String getSchema() {
        if (dataSourcesManager.getDefaultDataSource()
                              .isOfType(DatabaseSystem.POSTGRESQL)) {
            return "public";
        }

        if (dataSourcesManager.getDefaultDataSource()
                              .isOfType(DatabaseSystem.MSSQL)) {
            return "dbo";
        }

        return "PUBLIC";
    }

    private void expandSubviews(String schema, DatabasePerspective databasePerspective) {
        // some time is needed so that the tables is returned by the db metadata
        // this is applicable for databases like MSSQL
        SleepUtil.sleepSeconds(10);
        databasePerspective.refreshTables();

        databasePerspective.expandSubmenu(schema);

        databasePerspective.expandSubmenu("Tables");
    }

    private void assertAvailabilityOfSubitems(DatabasePerspective databasePerspective) {
        databasePerspective.assertSubmenu("Tables");
        databasePerspective.assertSubmenu("Views");
        databasePerspective.assertSubmenu("Procedures");
        databasePerspective.assertSubmenu("Functions");
        databasePerspective.assertSubmenu("Sequences");
    }

    private void assertInsertedRecord(DatabasePerspective databasePerspective) {
        databasePerspective.showTableContents(TEST_TABLE_NAME);

        // Assert if table id is 1 -> correct insertion
        databasePerspective.assertCellContent("1");
        dbAsserter.assertRowCount(TEST_TABLE_NAME, 1);
        dbAsserter.assertTableHasColumn(TEST_TABLE_NAME, "NAME");
        dbAsserter.assertRowHasColumnWithValue(TEST_TABLE_NAME, 0, "NAME", "John Smith");
    }

    private void createTestTable(DatabasePerspective databasePerspective) throws SQLException {
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSourcesManager.getDefaultDataSource());
        String createTableSql = dialect.create()
                                       .table(TEST_TABLE_NAME)
                                       .columnInteger("id", true)
                                       .columnVarchar("name", 30, false, false)
                                       .columnVarchar("address", 30, false, false)
                                       .build();
        databasePerspective.executeSql(createTableSql);
    }

    private void insertTestRecord(DatabasePerspective databasePerspective) throws SQLException {
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSourcesManager.getDefaultDataSource());
        String insertSql = dialect.insert()
                                  .into(TEST_TABLE_NAME)
                                  .column("id")
                                  .value("1")
                                  .column("name")
                                  .value("'John Smith'")
                                  .column("address")
                                  .value("'Sofia, Bulgaria'")
                                  .build();
        databasePerspective.executeSql(insertSql);
    }

}
