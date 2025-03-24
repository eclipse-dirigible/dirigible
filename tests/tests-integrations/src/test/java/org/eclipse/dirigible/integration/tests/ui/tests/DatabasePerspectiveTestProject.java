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
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class DatabasePerspectiveTestProject extends BaseTestProject {

    protected DatabasePerspectiveTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super("DatabasePerspectiveIT", ide, projectUtil, edmView);
    }

    @Override
    public void verify() throws Exception {
        DatabasePerspective databasePerspective = getIde().openDatabasePerspective();

        databasePerspective.executeSql("CREATE TABLE IF NOT EXISTS STUDENT (" + " id SERIAL PRIMARY KEY, " + " name TEXT NOT NULL, "
                + " address TEXT NOT NULL" + ");"); // Creating test table first to show in the database view

        expandSubviews(databasePerspective);
        assertAvailabilityOfSubitems(databasePerspective);

        databasePerspective.assertEmptyTable("STUDENT");
        databasePerspective.executeSql("INSERT INTO STUDENT VALUES (1, 'John Smith', 'Sofia, Bulgaria')");
        assertInsertedRecord(databasePerspective);
    }

    private void expandSubviews(DatabasePerspective databasePerspective) {
        String url = System.getenv("DIRIGIBLE_DATASOURCE_DEFAULT_URL");

        if (url != null && url.contains("postgresql"))
            databasePerspective.expandSubmenu("public");
        else
            databasePerspective.expandSubmenu("PUBLIC");

        databasePerspective.expandSubmenu("Tables");
        databasePerspective.refreshTables();
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
}
