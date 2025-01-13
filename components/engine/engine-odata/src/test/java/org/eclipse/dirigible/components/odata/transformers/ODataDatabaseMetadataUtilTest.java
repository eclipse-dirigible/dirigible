/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class DBMetadataUtilTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@ExtendWith(MockitoExtension.class)
public class ODataDatabaseMetadataUtilTest {

    /** The Constant CATALOG. */
    private static final String CATALOG = "Catalog";

    /** The Constant MY_TABLE_NAME. */
    private static final String MY_TABLE_NAME = "MY_TABLE";

    /** The Constant MY_SCHEMA_NAME. */
    private static final String MY_SCHEMA_NAME = "mySchema";

    /** The Constant COLUMN_NAME_LABEL. */
    private static final String COLUMN_NAME_LABEL = "COLUMN_NAME";

    /** The Constant COLUMN_TYPE_LABEL. */
    private static final String COLUMN_TYPE_LABEL = "TYPE_NAME";

    /** The Constant SQL_TO_EDM_TYPE_MAP. */
    private static final Map<String, String> SQL_TO_EDM_TYPE_MAP = new HashMap<>();

    static {
        SQL_TO_EDM_TYPE_MAP.put("TIME", "Edm.Time");
        SQL_TO_EDM_TYPE_MAP.put("DATE", "Edm.DateTime");
        SQL_TO_EDM_TYPE_MAP.put("SECONDDATE", "Edm.DateTime");
        SQL_TO_EDM_TYPE_MAP.put("TIMESTAMP", "Edm.DateTime");
        SQL_TO_EDM_TYPE_MAP.put("TINYINT", "Edm.Byte");
        SQL_TO_EDM_TYPE_MAP.put("SMALLINT", "Edm.Int16");
        SQL_TO_EDM_TYPE_MAP.put("INTEGER", "Edm.Int32");
        SQL_TO_EDM_TYPE_MAP.put("INT4", "Edm.Int32");
        SQL_TO_EDM_TYPE_MAP.put("BIGINT", "Edm.Int64");
        SQL_TO_EDM_TYPE_MAP.put("SMALLDECIMAL", "Edm.Decimal");
        SQL_TO_EDM_TYPE_MAP.put("DECIMAL", "Edm.Decimal");
        SQL_TO_EDM_TYPE_MAP.put("REAL", "Edm.Single");
        SQL_TO_EDM_TYPE_MAP.put("FLOAT", "Edm.Single");
        SQL_TO_EDM_TYPE_MAP.put("DOUBLE", "Edm.Double");
        SQL_TO_EDM_TYPE_MAP.put("VARCHAR", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("NVARCHAR", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("CHAR", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("NCHAR", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("BINARY", "Edm.Binary");
        SQL_TO_EDM_TYPE_MAP.put("VARBINARY", "Edm.Binary");
        SQL_TO_EDM_TYPE_MAP.put("BOOLEAN", "Edm.Boolean");
        SQL_TO_EDM_TYPE_MAP.put("BYTE", "Edm.Byte");
        SQL_TO_EDM_TYPE_MAP.put("BIT", "Edm.Byte");
        SQL_TO_EDM_TYPE_MAP.put("BLOB", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("NCLOB", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("CLOB", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("TEXT", "Edm.String");
        SQL_TO_EDM_TYPE_MAP.put("BINTEXT", "Edm.Binary");
        SQL_TO_EDM_TYPE_MAP.put("ALPHANUM", "Edm.String");
    }

    /** The connection. */
    @Mock
    private DirigibleConnection connection;

    /** The data source. */
    @Mock
    private DirigibleDataSource dataSource;

    /** The database meta data. */
    @Mock
    private DatabaseMetaData databaseMetaData;

    /** The datasources manager. */
    @Mock
    private DataSourcesManager datasourcesManager;

    /**
     * Sets the up.
     *
     * @throws SQLException the SQL exception
     */
    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);
        Mockito.when(datasourcesManager.getDefaultDataSource())
               .thenReturn(dataSource);
        Mockito.when(dataSource.getConnection())
               .thenReturn(connection);
        Mockito.when(connection.getCatalog())
               .thenReturn(CATALOG);
    }

    /**
     * Test get table metadata column type conversion.
     */
    // @Test
    // public void testGetTableMetadata_columnTypeConversion(){
    // SQL_TO_EDM_TYPE_MAP.forEach(this::testGetTableMetadata_columnTypeConversion);
    // }

    /**
     * Test get table metadata column conversion not supported type.
     */
    // @Test
    // public void testGetTableMetadata_columnConversionNotSupportedType(){
    // testGetTableMetadata_columnTypeConversion("NotSupportedSQLType", null);
    // }


    /**
     * Test artifact not found.
     *
     * @throws SQLException the SQL exception
     */
    // @Test
    // public void testArtifactNotFound() throws SQLException {
    // ODataDatabaseMetadataUtil odataDatabaseMetadataUtil = new ODataDatabaseMetadataUtil();
    // Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
    // mockDBMetaDataQuery_noResult(databaseMetaData.getTables(CATALOG, MY_SCHEMA_NAME, MY_TABLE_NAME,
    // null));
    //
    // Table tableMetadata = odataDatabaseMetadataUtil.getTableMetadata(MY_TABLE_NAME, MY_SCHEMA_NAME);
    // Assertions.assertNull(tableMetadata, "Unexpected metadata result for not existing DB artifact.");
    //
    // Mockito.verify(databaseMetaData).getTables(CATALOG, MY_SCHEMA_NAME, MY_TABLE_NAME, null);
    // Mockito.verifyNoMoreInteractions(databaseMetaData);
    // }

    // private void testGetTableMetadata_columnTypeConversion(String sqlType, String expectedEdmType) {
    // try {
    // Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
    //
    // mockDBMetaDataQuery_noResult(databaseMetaData.getPrimaryKeys(CATALOG, MY_SCHEMA_NAME,
    // MY_TABLE_NAME));
    // mockDBMetaDataQuery_noResult(databaseMetaData.getImportedKeys(CATALOG, MY_SCHEMA_NAME,
    // MY_TABLE_NAME));
    //
    // ResultSet tables = Mockito.mock(ResultSet.class);
    // Mockito.when(databaseMetaData.getTables(CATALOG, MY_SCHEMA_NAME, MY_TABLE_NAME,
    // null)).thenReturn(tables);
    // // to skip the PostgreSQL fallback logic
    // Mockito.when(tables.isBeforeFirst()).thenReturn(true);
    //
    // Mockito.when(tables.next()).thenReturn(true).thenReturn(false);
    // Mockito.when(tables.getString("TABLE_TYPE")).thenReturn("VIEW");
    //
    // ResultSet columns = Mockito.mock(ResultSet.class);
    // Mockito.when(databaseMetaData.getColumns(CATALOG, MY_SCHEMA_NAME, MY_TABLE_NAME,
    // null)).thenReturn(columns);
    // // to skip the PostgreSQL fallback logic
    // Mockito.when(columns.isBeforeFirst()).thenReturn(true);
    //
    // Mockito.when(columns.next()).thenReturn(true).thenReturn(false);
    // final String myColumnName = "MyColumn";
    //
    // Mockito.when(columns.getString(COLUMN_NAME_LABEL)).thenReturn(myColumnName);
    // Mockito.when(columns.getString(COLUMN_TYPE_LABEL)).thenReturn(sqlType);
    //
    // ODataDatabaseMetadataUtil odataDatabaseMetadataUtil = new ODataDatabaseMetadataUtil();
    // Table tableMetadata = odataDatabaseMetadataUtil.getTableMetadata(MY_TABLE_NAME, MY_SCHEMA_NAME);
    //
    // Assertions.assertEquals(1, tableMetadata.getColumns().size(), "Unexpected columns size");
    // Assertions.assertEquals(myColumnName, tableMetadata.getColumns().get(0).getName(), "Unexpected
    // column name");
    // Assertions.assertEquals(expectedEdmType, tableMetadata.getColumns().get(0).getType(), "Unexpected
    // column type");
    //
    // Mockito.verify(columns).getString(COLUMN_NAME_LABEL);
    // Mockito.verify(columns).getString(COLUMN_TYPE_LABEL);
    // Mockito.verify(columns, Mockito.times(2)).next();
    // Mockito.verify(columns).isBeforeFirst();
    // } catch (SQLException e) {
    // Assertions.fail("Fail to test column type conversion from [" + sqlType + "] to [" +
    // expectedEdmType + "] because of: " + e.getMessage());
    // }
    //
    // }

    // private void mockDBMetaDataQuery_noResult(ResultSet executedResultSet) throws SQLException {
    // ResultSet resultSet = Mockito.mock(ResultSet.class);
    // Mockito.when(executedResultSet).thenReturn(resultSet);
    // // to skip the PostgreSQL fallback logic
    // Mockito.when(resultSet.isBeforeFirst()).thenReturn(true);
    // Mockito.when(resultSet.next()).thenReturn(false);
    // }

    @SpringBootApplication
    static class TestConfiguration {
    }

}
