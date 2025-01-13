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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.database.persistence.PersistenceFactory;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.junit.Test;

/**
 * The Class PersistenceFactoryTest.
 */
public class PersistenceFactoryTest {

    /**
     * Creates the model from pojo.
     */
    @Test
    public void createModelFromPojo() {
        Customer customer = new Customer();
        PersistenceTableModel persistenceModel = PersistenceFactory.createModel(customer);
        assertEquals("CUSTOMERS", persistenceModel.getTableName());
        assertEquals("FACTORY", persistenceModel.getSchemaName());
        assertTrue(persistenceModel.getColumns()
                                   .size() == 4);
        PersistenceTableColumnModel persistenceCoulmnModelCustomerId = null;
        for (PersistenceTableColumnModel persistenceCoulmnModel : persistenceModel.getColumns()) {
            if ("CUSTOMER_ID".equals(persistenceCoulmnModel.getName())) {
                persistenceCoulmnModelCustomerId = persistenceCoulmnModel;
            }
        }
        assertNotNull(persistenceCoulmnModelCustomerId);
        assertEquals("INTEGER", persistenceCoulmnModelCustomerId.getType());
    }

    /**
     * Creates the model from json.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void createModelFromJson() throws IOException {
        InputStream in = PersistenceFactoryTest.class.getResourceAsStream("/Customer.json");
        try {
            String json = IOUtils.toString(in);
            PersistenceTableModel persistenceModel = PersistenceFactory.createModel(json);
            assertEquals("CUSTOMERS", persistenceModel.getTableName());
            assertEquals("FACTORY", persistenceModel.getSchemaName());
            assertTrue(persistenceModel.getColumns()
                                       .size() == 4);
            PersistenceTableColumnModel persistenceCoulmnModelCustomerId = null;
            for (PersistenceTableColumnModel persistenceCoulmnModel : persistenceModel.getColumns()) {
                if ("CUSTOMER_ID".equals(persistenceCoulmnModel.getName())) {
                    persistenceCoulmnModelCustomerId = persistenceCoulmnModel;
                }
            }
            assertNotNull(persistenceCoulmnModelCustomerId);
            assertEquals("INTEGER", persistenceCoulmnModelCustomerId.getType());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

}
