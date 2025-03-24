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
import org.eclipse.dirigible.tests.DatabasePerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DatabasePerspectiveIT extends UserInterfaceIntegrationTest {

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void testDatabaseFunctionality() {
        boolean postgreSQL = dataSourcesManager.getDefaultDataSource()
                                               .isOfType(DatabaseSystem.POSTGRESQL);
        String schema = postgreSQL ? "public" : "PUBLIC";
        DatabasePerspective databasePerspective = ide.openDatabasePerspective();

        createTestTable(databasePerspective); // Creating test table first to show in the database view

        expandSubviews(schema, databasePerspective);
        assertAvailabilityOfSubitems(databasePerspective);

        String tableName = postgreSQL ? "student" : "STUDENT";
        databasePerspective.assertEmptyTable(tableName);
        insertTestRecord(databasePerspective);
        assertInsertedRecord(databasePerspective);
    }

    private void expandSubviews(String schema, DatabasePerspective databasePerspective) {
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
        databasePerspective.showTableContents("STUDENT");

        // Assert if table id is 1 -> correct insertion
        databasePerspective.assertCellContent("1");
        databasePerspective.assertRowCount("STUDENT", 1);
        databasePerspective.assertTableHasColumn("STUDENT", "NAME");
        databasePerspective.assertRowHasColumnWithValue("STUDENT", 0, "NAME", "John Smith");
    }

    private void createTestTable(DatabasePerspective databasePerspective) {
        databasePerspective.executeSql("CREATE TABLE IF NOT EXISTS STUDENT (" + " id SERIAL PRIMARY KEY, " + " name TEXT NOT NULL, "
                + " address TEXT NOT NULL" + ");");
    }

    private void insertTestRecord(DatabasePerspective databasePerspective) {
        databasePerspective.executeSql("INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')");
    }

}
