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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.database.persistence.PersistenceManager;
import org.junit.Test;

/**
 * The Class PersistenceManagerEnumTest.
 */
public class PersistenceManagerEnumTest extends AbstractPersistenceManagerTest {

    /**
     * Ordered CRUD tests.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void orderedCrudTests() throws SQLException {
        PersistenceManager<Process> persistenceManager = new PersistenceManager<Process>();
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            // create table
            createTableForPojo(connection, persistenceManager);
            // check whether it is created successfully
            assertTrue(existsTable(connection, persistenceManager));
            // insert a record in the table for a pojo
            insertPojo(connection, persistenceManager);
            // insert a nullable record in the table for a pojo
            insertNullablePojo(connection, persistenceManager);
            // get the list of all the records
            findAllPojo(connection, persistenceManager);
            // drop the table
            dropTableForPojo(connection, persistenceManager);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Creates the table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void createTableForPojo(Connection connection, PersistenceManager<Process> persistenceManager) {
        persistenceManager.tableCreate(connection, Process.class);
    }

    /**
     * Exists table.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     * @return true, if successful
     */
    private boolean existsTable(Connection connection, PersistenceManager<Process> persistenceManager) {
        return persistenceManager.tableExists(connection, Process.class);
    }

    /**
     * Insert pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     * @throws SQLException the SQL exception
     */
    private void insertPojo(Connection connection, PersistenceManager<Process> persistenceManager) throws SQLException {
        Process process = new Process();
        process.setName("Process1");
        process.setTypeAsInt(Process.ProcessType.STARTED);
        process.setTypeAsString(Process.ProcessType.STARTED);
        persistenceManager.insert(connection, process);

        PreparedStatement preparedStatement = connection.prepareStatement("select * from PROCESSES");
        try {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                assertTrue(Process.ProcessType.STARTED.name()
                                                      .equals(resultSet.getString("PROCESS_TYPE_AS_STRING")));
                assertTrue(Process.ProcessType.STARTED.ordinal() == resultSet.getInt("PROCESS_TYPE_AS_INT"));
            }
        } finally {
            preparedStatement.close();
        }
    }

    /**
     * Insert nullable pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertNullablePojo(Connection connection, PersistenceManager<Process> persistenceManager) {
        Process process = new Process();
        process.setName("Process2");
        process.setTypeAsInt(null);
        process.setTypeAsString(null);
        persistenceManager.insert(connection, process);
    }

    /**
     * Find all pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void findAllPojo(Connection connection, PersistenceManager<Process> persistenceManager) {
        List<Process> list = persistenceManager.findAll(connection, Process.class);

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        Process order = list.get(0);
        assertEquals(Process.ProcessType.STARTED, order.getTypeAsInt());
        assertEquals(Process.ProcessType.STARTED, order.getTypeAsString());
        System.out.println(order.getId());

        order = list.get(1);
        assertNull(order.getTypeAsInt());
        assertNull(order.getTypeAsString());

        System.out.println(order.getId());

    }

    /**
     * Drop table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void dropTableForPojo(Connection connection, PersistenceManager<Process> persistenceManager) {
        persistenceManager.tableDrop(connection, Process.class);
    }

}
