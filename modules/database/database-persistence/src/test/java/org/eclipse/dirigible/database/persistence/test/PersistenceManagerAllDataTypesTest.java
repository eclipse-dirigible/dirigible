/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import org.eclipse.dirigible.database.persistence.PersistenceManager;
import org.junit.Test;

/**
 * The Persistence Manager All Data Types Test.
 */
public class PersistenceManagerAllDataTypesTest extends AbstractPersistenceManagerTest {

    /**
     * Ordered CRUD tests.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void orderedCrudTests() throws SQLException {
        PersistenceManager<AllDataTypes> persistenceManager = new PersistenceManager<AllDataTypes>();
        try (Connection connection = getDataSource().getConnection()) {
            // create table
            createTableForPojo(connection, persistenceManager);
            // check whether it is created successfully
            assertTrue(existsTable(connection, persistenceManager));
            try {
                // insert a record in the table for a pojo
                insertPojo(connection, persistenceManager);
                // retreive the record by the primary key
                findPojo(connection, persistenceManager);
            } finally {
                // drop the table
                dropTableForPojo(connection, persistenceManager);
            }
        }
    }

    /**
     * Creates the table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void createTableForPojo(Connection connection, PersistenceManager<AllDataTypes> persistenceManager) {
        persistenceManager.tableCreate(connection, AllDataTypes.class);
    }

    /**
     * Exists table.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     * @return true, if successful
     */
    private boolean existsTable(Connection connection, PersistenceManager<AllDataTypes> persistenceManager) {
        return persistenceManager.tableExists(connection, AllDataTypes.class);
    }

    /**
     * Insert pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertPojo(Connection connection, PersistenceManager<AllDataTypes> persistenceManager) {
        AllDataTypes allDataTypes = new AllDataTypes();
        allDataTypes.set_bigint(new BigInteger("1000000000"));
        allDataTypes.set_bit(true);
        allDataTypes.set_blob(new byte[] {1, 2, 3, 4, 5, 6});
        allDataTypes.set_boolean(true);
        allDataTypes.set_char("XXX");
        allDataTypes.set_date(new Date(123456));
        allDataTypes.set_decimal(new BigDecimal("10000000.00"));
        allDataTypes.set_double(12.34);
        allDataTypes.set_integer(1234);
        allDataTypes.set_real(12.34f);
        allDataTypes.set_smallint((short) 1);
        allDataTypes.set_time(new Time(123456));
        allDataTypes.set_timestamp(new Timestamp(123456));
        allDataTypes.set_tinyint((byte) 1);
        allDataTypes.set_varchar("Test");

        persistenceManager.insert(connection, allDataTypes);
    }

    /**
     * Find pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void findPojo(Connection connection, PersistenceManager<AllDataTypes> persistenceManager) {
        AllDataTypes allDataTypes = persistenceManager.find(connection, AllDataTypes.class, "Test");
        assertEquals("Test", allDataTypes.get_varchar());
    }

    /**
     * Drop table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void dropTableForPojo(Connection connection, PersistenceManager<AllDataTypes> persistenceManager) {
        persistenceManager.tableDrop(connection, AllDataTypes.class);
    }

}
