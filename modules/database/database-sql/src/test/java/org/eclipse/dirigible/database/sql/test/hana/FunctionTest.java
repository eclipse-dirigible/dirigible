/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.test.hana;

import org.eclipse.dirigible.database.sql.DatabaseArtifactTypes;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.hana.HanaSqlDialect;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class FunctionTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Connection mockConnection;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseMetaData mockDatabaseMetaData;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ResultSet mockResultSet;

    @Before
    public void openMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void checkIfFunctionExist() throws SQLException {
        String funcName = "\"MYSCHEMA\".\"namespace.path::MyFunction\"";
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockDatabaseMetaData.getFunctions(null, null, funcName)).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                .exists(mockConnection, funcName, DatabaseArtifactTypes.FUNCTION);
        assertTrue(exist);
    }

    @Test
    public void checkIfFunctionDoesNotExist() throws SQLException {
        String funcName = "\"MYSCHEMA\".\"namespace.path::MyFunction\"";
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
        when(mockDatabaseMetaData.getFunctions(null, null, funcName)).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        boolean exist = SqlFactory.getNative(new HanaSqlDialect())
                .exists(mockConnection, "\"MYSCHEMA\".\"namespace.path::MyFunction\"", DatabaseArtifactTypes.FUNCTION);
        assertFalse(exist);
    }

}
