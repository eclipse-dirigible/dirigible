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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.dirigible.database.persistence.PersistenceManager;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Persistence Manager Generated Value Table Test.
 */
public class PersistenceManagerGeneratedValueIdentityTest extends AbstractPersistenceManagerTest {

    /** The Constant SUPPORTED_DIALECTS. */
    static final List<String> SUPPORTED_DIALECTS = Arrays.asList("sybase", "h2", "derby", "hana");

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceManagerGeneratedValueIdentityTest.class);

    /**
     * Ordered CRUD tests.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void orderedCrudTests() throws SQLException {
        String database = System.getProperty("database");
        if (database == null) {
            database = "h2";
        }
        if (!SUPPORTED_DIALECTS.contains(database)) {
            if (logger.isWarnEnabled()) {
                logger.warn("Skipped IDENTITY test for database: " + database);
            }
            return;
        }

        PersistenceManager<Offer> persistenceManager = new PersistenceManager<Offer>();
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
            // insert a record in the table for a pojo
            insertThirdPojo(connection, persistenceManager);
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
    private void createTableForPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        persistenceManager.tableCreate(connection, Offer.class);
    }

    /**
     * Exists table.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     * @return true, if successful
     */
    private boolean existsTable(Connection connection, PersistenceManager<Offer> persistenceManager) {
        return persistenceManager.tableExists(connection, Offer.class);
    }

    /**
     * Insert pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        Offer offer = new Offer();
        offer.setSubject("Subject 1");
        persistenceManager.insert(connection, offer);
        assertNotEquals(0, offer.getId());
    }

    /**
     * Insert second pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertSecondPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        Offer offer = new Offer();
        offer.setSubject("Subject 2");
        persistenceManager.insert(connection, offer);
    }

    /**
     * Insert third pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void insertThirdPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        Offer offer = new Offer();
        offer.setSubject("Subject 3");
        persistenceManager.insert(connection, offer);
    }

    /**
     * Find all pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void findAllPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        List<Offer> list = persistenceManager.findAll(connection, Offer.class);

        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        Offer offer = list.get(0);
        assertEquals("Subject 1", offer.getSubject());

        System.out.println(offer.getId());

    }

    /**
     * Drop table for pojo.
     *
     * @param connection the connection
     * @param persistenceManager the persistence manager
     */
    private void dropTableForPojo(Connection connection, PersistenceManager<Offer> persistenceManager) {
        persistenceManager.tableDrop(connection, Offer.class);
    }

}
