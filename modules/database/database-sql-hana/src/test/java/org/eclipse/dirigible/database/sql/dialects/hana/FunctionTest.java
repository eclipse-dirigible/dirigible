/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.dirigible.database.sql.DatabaseArtifactTypes;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * The Class FunctionTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class FunctionTest {

    /** The mock connection. */
    @Mock
    private Connection mockConnection;

    /** The mock database meta data. */
    @Mock
    private DatabaseMetaData mockDatabaseMetaData;

    /** The mock result set. */
    @Mock
    private ResultSet mockResultSet;

    /**
     * Check if function exist.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void checkIfFunctionExist() throws SQLException {
        String funcName = "\"namespace.path::MyFunction\"";
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockDatabaseMetaData.getFunctions(null, "MYSCHEMA", funcName)).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                                  .exists(mockConnection, "MYSCHEMA", funcName, DatabaseArtifactTypes.FUNCTION);
        assertTrue(exist);
    }

    /**
     * Check if function does not exist.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void checkIfFunctionDoesNotExist() throws SQLException {
        String funcName = "\"namespace.path::MyFunction\"";
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockDatabaseMetaData.getFunctions(null, "MYSCHEMA", funcName)).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                                  .exists(mockConnection, "MYSCHEMA", funcName, DatabaseArtifactTypes.FUNCTION);
        assertFalse(exist);
    }

}
