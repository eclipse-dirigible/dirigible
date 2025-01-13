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

import org.eclipse.dirigible.database.sql.DatabaseArtifactTypes;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * The Class SynonymTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class SynonymTest {

    /** The mock connection. */
    @Mock
    private Connection mockConnection;

    /** The mock database meta data. */
    @Mock
    private DatabaseMetaData mockDatabaseMetaData;

    /** The mock prepare statement. */
    @Mock
    private PreparedStatement mockPrepareStatement;

    /** The mock result set. */
    @Mock
    private ResultSet mockResultSet;

    /**
     * Execute create synonym.
     */
    @Test
    public void executeCreateSynonym() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .synonym("CUSTOMERS_SEQUENCE")
                               .forSource("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE SYNONYM \"CUSTOMERS_SEQUENCE\" FOR \"CUSTOMERS\"", sql);
    }

    /**
     * Execute drop synonym.
     */
    @Test
    public void executeDropSynonym() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .drop()
                               .synonym("CUSTOMERS_SEQUENCE")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP SYNONYM \"CUSTOMERS_SEQUENCE\"", sql);
    }

    /**
     * Check if synonym exist.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void checkIfSynonymExist() throws SQLException {
        String synonymName = "namespace.path::MySynonym";
        String schema = "MySchema";
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockDatabaseMetaData.getTables(null, schema, synonymName, new String[] {ISqlKeywords.KEYWORD_SYNONYM})).thenReturn(
                mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                                  .exists(mockConnection, schema, synonymName, DatabaseArtifactTypes.SYNONYM);
        assertTrue(exist);
    }

    /**
     * Check if synonym does not exist.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void checkIfSynonymDoesNotExist() throws SQLException {
        String synonymName = "namespace.path::MySynonym";
        String schema = "MySchema";
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                                  .exists(mockConnection, schema, synonymName, DatabaseArtifactTypes.SYNONYM);
        assertFalse(exist);
    }

}
