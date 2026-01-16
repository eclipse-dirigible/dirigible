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
 * The Class SequenceTest.
 */
public class SequenceTest {

    /**
     * Creates the sequence.
     */
    @Test
    public void createSequence() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .create()
                               .sequence("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE SEQUENCE \"CUSTOMERS_SEQUENCE\"", sql);
    }

    /**
     * Alter sequence.
     */
    @Test
    public void alterSequence() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .alter()
                               .sequence("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER SEQUENCE \"CUSTOMERS_SEQUENCE\"", sql);
    }

    /**
     * Drop sequnce.
     */
    @Test
    public void dropSequnce() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .drop()
                               .sequence("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP SEQUENCE \"CUSTOMERS_SEQUENCE\"", sql);
    }

    /**
     * Nextval sequnce.
     */
    @Test
    public void nextvalSequnce() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .nextval("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT NEXT VALUE FOR \"CUSTOMERS_SEQUENCE\"", sql);
    }

}
