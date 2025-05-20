/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.db;

import org.assertj.db.api.Assertions;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.db.DatabaseFacade;
import org.eclipse.dirigible.components.data.management.service.DatabaseDefinitionService;
import org.eclipse.dirigible.components.data.sources.config.SystemDataSourceName;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.db.DBAsserter;
import org.eclipse.dirigible.tests.framework.util.JsonAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseFacadeIT extends IntegrationTest {

    private static final String ID_COLUMN = "Id";
    private static final String NAME_COLUMN = "Name";
    private static final String BIRTHDAY_COLUMN = "Birthday";
    private static final String BIRTHDAY_STRING_COLUMN = "BirthdayString";
    private static final String TEST_TABLE = "TEACHERS";

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DBAsserter dbAsserter;

    @SystemDataSourceName
    @Autowired
    private String systemDataSource;


    @Nested
    class UpdateTest {
        @Test
        void testUpdate() throws Throwable {
            String updateSql = getDialect().update()
                                           .table(TEST_TABLE)
                                           .set(ID_COLUMN, "?")
                                           .build();
            String parametersJson = "[12]";
            int updatedRows = DatabaseFacade.update(updateSql, parametersJson);
            assertThat(updatedRows).isEqualTo(1);

            String result = queryTestTable();
            assertPreparedResult(12, result);
        }

        @Test
        void testUpdateNamed() throws Throwable {
            String updateSql = getDialect().update()
                                           .table(TEST_TABLE)
                                           .set(ID_COLUMN, ":id")
                                           .build();
            String parametersJson = """
                    [
                        {
                            "name": "id",
                            "type": "INT",
                            "value": 12
                        }
                    ]
                    """;
            int updatedRows = DatabaseFacade.updateNamed(updateSql, parametersJson);
            assertThat(updatedRows).isEqualTo(1);

            String result = queryTestTable();
            assertPreparedResult(12, result);
        }
    }


    @Nested
    class SequenceTest {

        @Test
        void testCreateSequenceByName() {
            assertDoesNotThrow(() -> DatabaseFacade.createSequence("TEST_SEQ_01"));
        }

        @Test
        void testCreateSequenceByNameAndStart() {
            assertDoesNotThrow(() -> DatabaseFacade.createSequence("TEST_SEQ_02", 100));
        }

        @Test
        void testCreateSequenceByNameStartAndDataSourceName() {
            assertDoesNotThrow(() -> DatabaseFacade.createSequence("TEST_SEQ_03", 200, systemDataSource));
        }

        @Test
        void testDropSequenceByName() {
            assertDoesNotThrow(() -> {
                DatabaseFacade.createSequence("TEST_SEQ_03");
                DatabaseFacade.dropSequence("TEST_SEQ_03");
            });
        }

        @Test
        void testDropSequenceByNameAndDataSourceName() {
            assertDoesNotThrow(() -> {
                DatabaseFacade.createSequence("TEST_SEQ_04", 300, systemDataSource);
                DatabaseFacade.dropSequence("TEST_SEQ_04", systemDataSource);
            });
        }
    }


    @Nested
    class InsertTest {

        @Test
        void testInsertWithoutParams() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .value("1000")
                                           .build();
            DatabaseFacade.insert(insertSql, null, null);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1000);
        }

        @Test
        void testInsertWithParamsArray() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .column(NAME_COLUMN)
                                           .column(BIRTHDAY_COLUMN)
                                           .column(BIRTHDAY_STRING_COLUMN)
                                           .build();
            String parametersJson = createParamsJson(300, "Ivan", "2000-01-21", "20020222");
            DatabaseFacade.insert(insertSql, parametersJson, null);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(300)
                      .value(NAME_COLUMN)
                      .isEqualTo("Ivan")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2000-01-21"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20020222");
        }

        @Test
        void testInsertWithParamsObjectsArray() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .column(NAME_COLUMN)
                                           .column(BIRTHDAY_COLUMN)
                                           .column(BIRTHDAY_STRING_COLUMN)
                                           .build();
            String parametersJson = """
                    [
                        {
                            "value": 1700
                        },
                        {
                            "value": "testInsertWithParamsObjectsArray"
                        },
                        {
                            "value": "2005-05-25"
                        },
                        {
                            "value": "20060626"
                        }
                    ]
                    """;

            DatabaseFacade.insert(insertSql, parametersJson, null);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1700)
                      .value(NAME_COLUMN)
                      .isEqualTo("testInsertWithParamsObjectsArray")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2005-05-25"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20060626");
        }

        @Test
        void testInsertNamed() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .column(NAME_COLUMN)
                                           .column(BIRTHDAY_COLUMN)
                                           .column(BIRTHDAY_STRING_COLUMN)
                                           .value(":id")
                                           .value(":name")
                                           .value(":birthday")
                                           .value(":birthdayString")
                                           .build();
            String parametersJson = """
                    [
                        {
                            "name": "id",
                            "type": "INT",
                            "value": 700
                        },
                        {
                            "name": "name",
                            "type": "VARCHAR",
                            "value": "Ivan"
                        },
                        {
                            "name": "birthday",
                            "type": "DATE",
                            "value": "2000-01-21"
                        },
                           {
                            "name": "birthdayString",
                            "type": "VARCHAR",
                            "value": "20020222"
                        }
                    ]
                    """;
            DatabaseFacade.insertNamed(insertSql, parametersJson, null);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(700)
                      .value(NAME_COLUMN)
                      .isEqualTo("Ivan")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2000-01-21"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20020222");
        }

    }


    @Nested
    class QueryTest {
        @Test
        void testQuery() throws Throwable {
            String result = queryTestTable();

            assertPreparedResult(result);
        }

        @Test
        void testQueryNamed() throws Throwable {
            String selectSql = getDialect().select()
                                           .from(TEST_TABLE)
                                           .where(ID_COLUMN + " = 0")
                                           .build();
            String result = DatabaseFacade.queryNamed(selectSql);

            assertPreparedResult(result);
        }

        @Test
        void testQueryNamedWithParams() throws Throwable {
            ISqlDialect dialect = getDialect();
            String selectSql = dialect.select()
                                      .from(TEST_TABLE)
                                      .build();
            selectSql = selectSql + "WHERE " + dialect.getEscapeSymbol() + ID_COLUMN + dialect.getEscapeSymbol() + "=:id";

            String parametersJson = """
                    [
                        {
                            "name": "id",
                            "type": "INT",
                            "value": 0
                        }
                    ]
                    """;
            String result = DatabaseFacade.queryNamed(selectSql, parametersJson, null);

            assertPreparedResult(result);
        }

    }


    @Nested
    class InsertManyTest {
        @Test
        void testInsertMany() throws Throwable {
            Object[][] params = {//
                    {1, "John", "2000-12-20", "20001121"}, //
                    {2, "Mary", "2001-11-21", "20001222"}//
            };
            insertMany(params);

            Table table = createAssertTestTable();

            Assertions.assertThat(table)
                      .hasNumberOfRows(3)

                      .row(0)
                      .value(ID_COLUMN)
                      .isEqualTo(0)
                      .value(NAME_COLUMN)
                      .isEqualTo("Peter")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2025-01-20"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("2024-02-22")

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1)
                      .value(NAME_COLUMN)
                      .isEqualTo("John")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2000-12-20"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20001121")

                      .row(2)
                      .value(ID_COLUMN)
                      .isEqualTo(2)
                      .value(NAME_COLUMN)
                      .isEqualTo("Mary")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2001-11-21"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20001222");
        }

        private void insertMany(Object[][] params) throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .column(NAME_COLUMN)
                                           .column(BIRTHDAY_COLUMN)
                                           .column(BIRTHDAY_STRING_COLUMN)
                                           .build();
            String parametersJson = createMultiParamsJson(params);
            DatabaseFacade.insertMany(insertSql, parametersJson, null);
        }
    }

    private static String createMultiParamsJson(Object[][] params) {
        return GsonHelper.toJson(params);
    }

    @BeforeEach
    void setUp() throws SQLException {
        deleteTestResources();

        createTestTable();
        insertTestRecord();
    }

    private void deleteTestResources() throws SQLException {
        dropTableIfExists(TEST_TABLE);
    }

    private void dropTableIfExists(String tableName) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            SqlFactory sqlFactory = SqlFactory.getNative(connection);
            if (sqlFactory.existsTable(connection, TEST_TABLE)) {
                String dropSql = sqlFactory.drop()
                                           .table(TEST_TABLE)
                                           .generate();
                PreparedStatement preparedStatement = connection.prepareStatement(dropSql);
                preparedStatement.executeUpdate();
            }

        }
    }

    private void createTestTable() throws SQLException {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
        String createTableSql = dialect.create()
                                       .table(TEST_TABLE)
                                       .columnInteger(ID_COLUMN)
                                       .columnVarchar(NAME_COLUMN, 50)
                                       .columnDate(BIRTHDAY_COLUMN)
                                       .columnVarchar(BIRTHDAY_STRING_COLUMN, 20)
                                       .build();
        try (DirigibleConnection connection = defaultDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(createTableSql)) {
            preparedStatement.executeUpdate();
        }
    }

    private void insertTestRecord() throws SQLException {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();

        ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
        String insertSql = dialect.insert()
                                  .into(TEST_TABLE)
                                  .column(ID_COLUMN)
                                  .column(NAME_COLUMN)
                                  .column(BIRTHDAY_COLUMN)
                                  .column(BIRTHDAY_STRING_COLUMN)
                                  .build();

        try (DirigibleConnection connection = defaultDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
            preparedStatement.setInt(1, 0);
            preparedStatement.setString(2, "Peter");
            preparedStatement.setDate(3, Date.valueOf("2025-01-20"));
            preparedStatement.setString(4, "2024-02-22");

            preparedStatement.executeUpdate();
        }
    }

    @Test
    void testGetDataSources() {
        String dataSources = DatabaseFacade.getDataSources();

        assertThat(dataSources).isNotBlank();
    }

    @Test
    void testGet() {
        DatabaseFacade databaseFacade = DatabaseFacade.get();

        assertThat(databaseFacade).isNotNull();
    }

    @Test
    void testGetDatabaseDefinitionService() {
        DatabaseDefinitionService databaseDefinitionService = DatabaseFacade.get()
                                                                            .getDatabaseDefinitionService();

        assertThat(databaseDefinitionService).isNotNull();
    }

    @Test
    void testGetDefaultDataSource() {
        DirigibleDataSource defaultDataSource = DatabaseFacade.getDefaultDataSource();

        assertThat(defaultDataSource).isNotNull();
    }

    @Test
    void testGetDataSourcesManager() {
        DataSourcesManager dsManager = DatabaseFacade.get()
                                                     .getDataSourcesManager();

        assertThat(dsManager).isNotNull();
    }

    @Test
    void testGetMetadata() throws Throwable {
        String metadata = DatabaseFacade.getMetadata();

        assertThat(metadata).isNotBlank();
    }

    @Test
    void testGetProductName() throws Throwable {
        String productName = DatabaseFacade.getProductName();

        assertThat(productName).isNotBlank();
    }

    @Test
    void testGetProductNameByName() throws Throwable {
        String productName = DatabaseFacade.getProductName(systemDataSource);

        assertThat(productName).isNotBlank();
    }

    private static void assertPreparedResult(String result) {
        assertPreparedResult(0, result);
    }

    private static void assertPreparedResult(int id, String result) {
        String expectedResult = "[{\"Id\":" + id + ",\"Name\":\"Peter\",\"Birthday\":\"2025-01-20\",\"BirthdayString\":\"2024-02-22\"}]";
        JsonAsserter.assertEquals(expectedResult, result);
    }

    private Table createAssertTestTable() {
        return dbAsserter.getDefaultDbTable(TEST_TABLE);
    }

    private static String createParamsJson(Object... params) {
        return GsonHelper.toJson(params);
    }

    private String queryTestTable() throws Throwable {
        String selectQuery = getDialect().select()
                                         .from(TEST_TABLE)
                                         .build();
        return DatabaseFacade.query(selectQuery);
    }

    private ISqlDialect getDialect() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();

        return SqlDialectFactory.getDialect(dataSource);
    }

    @Test
    void testGetConnection() throws Throwable {
        try (DirigibleConnection connection = DatabaseFacade.getConnection()) {
            assertThat(connection).isNotNull();
        }
    }

    @Test
    void testGetConnectionByDataSourceName() throws Throwable {
        try (DirigibleConnection connection = DatabaseFacade.getConnection(systemDataSource)) {
            assertThat(connection).isNotNull();
        }
    }

    @Disabled("To be implemented")
    @Test
    void testNextval() {}

    @Test
    void testGetDefaultSqlFactory() {
        SqlFactory sqlFactory = DatabaseFacade.getDefault();

        assertThat(sqlFactory).isNotNull();
    }

    @Test
    void testGetNative() throws SQLException {
        try (DirigibleConnection connection = dataSourcesManager.getDefaultDataSource()
                                                                .getConnection()) {
            SqlFactory sqlFactory = DatabaseFacade.getNative(connection);

            assertThat(sqlFactory).isNotNull();
        }
    }

    @Disabled("To be implemented")
    @Test
    void testReadBlobValue() {}

    @Disabled("To be implemented")
    @Test
    void testReadByteStream() {}
}
