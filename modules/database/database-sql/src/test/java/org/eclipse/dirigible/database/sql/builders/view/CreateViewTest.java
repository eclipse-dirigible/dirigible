/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableTest;
import org.junit.Test;

/**
 * The Class CreateViewTest.
 */
public class CreateViewTest extends CreateTableTest {

    /**
     * Creates the view as select case sensitive.
     */
    @Test
    public void createViewAsSelectCaseSensitive() {
        String sql = SqlFactory.getDefault()
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
     * Creates the view as select with join and alias.
     */
    @Test
    public void createViewWithJoinAndAlias() {
        createTableGeneric();
        String sql = SqlFactory.getDefault()
                               .create()
                               .view("CUSTOMERS_VIEW")
                               .column("ID")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .asSelect(SqlFactory.getDefault()
                                                   .select()
                                                   .column("*")
                                                   .from("CUSTOMERS", "CUST")
                                                   .innerJoin("EMPLOYEE", "CUST.ID = EMP.ID", "EMP")
                                                   .build())
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE VIEW \"CUSTOMERS_VIEW\" ( \"ID\" , \"FIRST_NAME\" , \"LAST_NAME\" ) AS SELECT * FROM \"CUSTOMERS\" "
                + "AS \"CUST\" INNER JOIN \"EMPLOYEE\" AS \"EMP\" ON \"CUST\".\"ID\" = \"EMP\".\"ID\"", sql);
    }
}
