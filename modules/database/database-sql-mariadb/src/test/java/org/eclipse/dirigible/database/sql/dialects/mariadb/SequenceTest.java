/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mariadb;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The Class SequenceTest.
 */
public class SequenceTest {

    /**
     * Creates the sequence.
     */
    @Test
    public void createSequence() {
        try {
            SqlFactory.getNative(new MariaDBSqlDialect())
                      .create()
                      .sequence("CUSTOMERS_SEQUENCE")
                      .build();
        } catch (Exception e) {
            return;
        }

        fail("Does MariaDB support Sequences?");
    }

    /**
     * Alter sequence.
     */
    @Test
    public void alterSequence() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .alter()
                               .sequence("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER SEQUENCE `CUSTOMERS_SEQUENCE`", sql);
    }

    /**
     * Drop sequnce.
     */
    @Test
    public void dropSequnce() {
        try {
            SqlFactory.getNative(new MariaDBSqlDialect())
                      .drop()
                      .sequence("CUSTOMERS_SEQUENCE")
                      .build();
        } catch (Exception e) {
            return;
        }

        fail("Does MariaDB support Sequences?");
    }

    /**
     * Nextval sequnce.
     */
    @Test
    public void nextvalSequnce() {
        try {
            SqlFactory.getNative(new MariaDBSqlDialect())
                      .nextval("CUSTOMERS_SEQUENCE")
                      .build();
        } catch (Exception e) {
            return;
        }

        fail("Does MariaDB support Sequences?");
    }

}
