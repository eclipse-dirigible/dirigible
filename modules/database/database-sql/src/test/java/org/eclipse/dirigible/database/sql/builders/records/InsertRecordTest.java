/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class InsertTest.
 */
public class InsertRecordTest {

    /**
     * Insert simple.
     */
    @Test
    public void insertSimple() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES (?, ?)", sql);
    }

    /**
     * Insert simple case sensitive.
     */
    @Test
    public void insertSimpleCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES (?, ?)", sql);

    }

    /**
     * Insert values.
     */
    @Test
    public void insertValues() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .value("?")
                               .value("'Smith'")
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES (?, 'Smith')", sql);
    }

    /**
     * Insert values case sensitive.
     */
    @Test
    public void insertValuesCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .value("?")
                               .value("'Smith'")
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") VALUES (?, 'Smith')", sql);
    }

    /**
     * Insert select.
     */
    @Test
    public void insertSelect() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .select(SqlFactory.getDefault()
                                                 .select()
                                                 .column("*")
                                                 .from("SUPPLIERS")
                                                 .build())
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") SELECT * FROM \"SUPPLIERS\"", sql);
    }

    /**
     * Insert select case sensitive.
     */
    @Test
    public void insertSelectCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .insert()
                               .into("CUSTOMERS")
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .select(SqlFactory.getDefault()
                                                 .select()
                                                 .column("*")
                                                 .from("SUPPLIERS")
                                                 .build())
                               .build();

        assertNotNull(sql);
        assertEquals("INSERT INTO \"CUSTOMERS\" (\"FIRST_NAME\", \"LAST_NAME\") SELECT * FROM \"SUPPLIERS\"", sql);

    }
}
