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

import org.eclipse.dirigible.tests.DatabasePerspective;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;

class DatabasePerspectiveIT extends UserInterfaceIntegrationTest {
    private DatabasePerspective databasePerspective;

    @Test
    void testDatabaseFunctionality() {
        this.databasePerspective = ide.openDatabasePerspective();

        createTestTable(); // Creating test table first to show in the database view

        expandSubviews();
        assertAvailabilityOfSubitems();

        databasePerspective.assertEmptyTable("STUDENT");
        insertTestRecord();
        assertInsertedRecord();
    }

    private void expandSubviews() {
        String url = System.getenv("DIRIGIBLE_DATASOURCE_DEFAULT_URL");

        if (url != null && url.contains("postgresql"))
            databasePerspective.expandSubmenu("public");
        else
            databasePerspective.expandSubmenu("PUBLIC");

        databasePerspective.expandSubmenu("Tables");
        databasePerspective.refreshTables();
    }

    private void assertAvailabilityOfSubitems() {
        databasePerspective.assertSubmenu("Tables");
        databasePerspective.assertSubmenu("Views");
        databasePerspective.assertSubmenu("Procedures");
        databasePerspective.assertSubmenu("Functions");
        databasePerspective.assertSubmenu("Sequences");
    }

    private void assertInsertedRecord() {
        databasePerspective.showTableContents("STUDENT");

        // Assert if table id is 1 -> correct insertion
        databasePerspective.assertCellContent("1");
        databasePerspective.assertRowCount("STUDENT", 1);
        databasePerspective.assertTableHasColumn("STUDENT", "NAME");
        databasePerspective.assertRowHasColumnWithValue("STUDENT", 0, "NAME", "John Smith");
    }

    private void createTestTable() {
        databasePerspective.executeSql("CREATE TABLE IF NOT EXISTS STUDENT (" + " id SERIAL PRIMARY KEY, " + " name TEXT NOT NULL, "
                + " address TEXT NOT NULL" + ");");
    }

    private void insertTestRecord() {
        databasePerspective.executeSql("INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')");
    }


}
