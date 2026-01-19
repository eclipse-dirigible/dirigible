/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Class CreateViewTest.
 */
public class CreateViewTest extends CreateTableTest {

    /**
     * Creates the view as select.
     */
    @Test
    public void createViewAsSelect() {
        createTableGeneric();
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .create()
                               .view("CUSTOMERS_VIEW")
                               .column("ID")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .asSelect(SqlFactory.getDefault()
                                                   .select()
                                                   .column("*")
                                                   .from("CUSTOMERS")
                                                   .build())
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE VIEW \"CUSTOMERS_VIEW\" ( \"ID\" , \"FIRST_NAME\" , \"LAST_NAME\" ) AS SELECT * FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Creates the view as values.
     */
    @Test
    public void createViewAsValues() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .create()
                               .view("STATES")
                               .column("STATE")
                               .asValues("'STARTED', 'STOPPED', 'FAILED', 'IN PROCESS'")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE VIEW \"STATES\" ( \"STATE\" ) AS SELECT 'STARTED' UNION ALL SELECT  'STOPPED' UNION ALL SELECT  'FAILED' UNION ALL SELECT  'IN PROCESS' ",
                sql);
    }

}
