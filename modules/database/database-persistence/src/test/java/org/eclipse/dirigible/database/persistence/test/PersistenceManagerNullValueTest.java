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
import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.PersistenceManager;
import org.junit.Test;

/**
 * The Persistence Manager Generated Value Table Test.
 */
public class PersistenceManagerNullValueTest extends AbstractPersistenceManagerTest {

    /**
     * Ordered CRUD tests.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void orderedCrudTests() throws SQLException {
        PersistenceManager<MultiOrder> persistenceManager = new PersistenceManager<MultiOrder>();
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            // create table
            createTableForPojo(connection, persistenceManager);
            // check whether it is created successfully
            assertTrue(existsTable(connection, persistenceManager));
            // insert a record in the table for a pojo
            insertPojo(connection, persistenceManager);
            // insert a record in the table for a pojo
            insertSecondPojo(connection, persistenceManager);
            // get the list of all the records
            findAllPojo(connection, persistenceManager);
            // update a record in the table for a pojo
            updatePojo(connection, persistenceManager);
            // update a record in the table for a pojo with null key
            updatePojoWithNullKey(connection, persistenceManager);
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
    private void createTableForPojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        persistenceManager.tableCreate(connection, MultiOrder.class);
    }

    /**
     * Exists table.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     * @return true, if successful
     */
    private boolean existsTable(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        return persistenceManager.tableExists(connection, MultiOrder.class);
    }

    /**
     * Insert pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertPojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        MultiOrder order = new MultiOrder();
        order.setSubject("Subject 1");
        persistenceManager.insert(connection, order);
    }

    /**
     * Insert second pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertSecondPojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        MultiOrder order = new MultiOrder();
        order.setSubject("Subject 2");
        order.setAmount(100L);
        order.setDescription("Description 2");
        persistenceManager.insert(connection, order);
    }

    /**
     * Find all pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void findAllPojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        List<MultiOrder> list = persistenceManager.findAll(connection, MultiOrder.class);

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
        MultiOrder order = list.get(0);
        assertEquals("Subject 1", order.getSubject());
        assertNull(order.getAmount());
        assertNull(order.getDescription());

        order = list.get(1);
        assertEquals("Subject 2", order.getSubject());
        assertEquals(new Long(100), order.getAmount());
        assertEquals("Description 2", order.getDescription());

        System.out.println(order.getId());

    }

    /**
     * Update pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void updatePojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        List<MultiOrder> list = persistenceManager.findAll(connection, MultiOrder.class);
        MultiOrder order = list.get(0);
        order.setDescription("New description");
        persistenceManager.update(connection, order);

        order = persistenceManager.find(connection, MultiOrder.class, order.getId());
        assertNotNull(order);
        assertEquals("New description", order.getDescription());
    }

    /**
     * Update pojo with null key.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void updatePojoWithNullKey(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        try {
            List<MultiOrder> list = persistenceManager.findAll(connection, MultiOrder.class);
            MultiOrder order = list.get(0);
            order.setDescription("New description");
            persistenceManager.update(connection, order);
        } catch (Exception e) {
            assertEquals(PersistenceException.class, e.getClass());
            assertEquals("The key for update cannot be null.", e.getMessage());
        }
    }

    /**
     * Drop table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void dropTableForPojo(Connection connection, PersistenceManager<MultiOrder> persistenceManager) {
        persistenceManager.tableDrop(connection, MultiOrder.class);
    }

}
