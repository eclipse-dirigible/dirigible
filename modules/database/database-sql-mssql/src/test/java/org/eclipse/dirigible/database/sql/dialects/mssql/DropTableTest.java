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
 * The Class DropTableTest.
 */
public class DropTableTest {

    /**
     * Drop table.
     */
    @Test
    public void dropTable() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .drop()
                               .table("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP TABLE \"CUSTOMERS\"", sql);
    }

}
