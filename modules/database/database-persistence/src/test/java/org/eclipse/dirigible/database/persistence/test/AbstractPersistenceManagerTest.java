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

import static org.junit.Assert.fail;

import javax.sql.DataSource;

import org.eclipse.dirigible.database.h2.H2Database;
import org.junit.Before;

/**
 * The Class AbstractPersistenceManagerTest.
 */
public class AbstractPersistenceManagerTest {

    /** The data source. */
    private DataSource dataSource = null;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        try {
            H2Database h2Database = new H2Database();
            this.dataSource = h2Database.getDataSource("target/h2");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Gets the data source.
     *
     * @return the data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

}
