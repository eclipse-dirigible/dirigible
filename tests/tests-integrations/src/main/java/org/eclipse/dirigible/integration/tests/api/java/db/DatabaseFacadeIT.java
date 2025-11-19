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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

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
import org.h2.jdbc.JdbcSQLDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

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
        void testUpdateWithParamsArray() throws Throwable {
            String updateSql = getDialect().update()
                                           .table(TEST_TABLE)
                                           .set(ID_COLUMN, "?")
                                           .set(NAME_COLUMN, "?")
                                           .set(BIRTHDAY_COLUMN, "?")
                                           .set(BIRTHDAY_STRING_COLUMN, "?")
                                           .where(ID_COLUMN + " = 0")
                                           .build();
            String parametersJson = """
                    [12000, "testUpdateWithParamsArray", "2009-09-29", "20101030"]
                    """;
            int updatedRows = DatabaseFacade.update(updateSql, parametersJson);
            assertThat(updatedRows).isEqualTo(1);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(1)

                      .row(0)
                      .value(ID_COLUMN)
                      .isEqualTo(12000)
                      .value(NAME_COLUMN)
                      .isEqualTo("testUpdateWithParamsArray")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2009-09-29"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20101030");
        }

        @Test
        void testUpdateWithParamsObjectsArray() throws Throwable {
            String updateSql = getDialect().update()
                                           .table(TEST_TABLE)
                                           .set(ID_COLUMN, "?")
                                           .set(NAME_COLUMN, "?")
                                           .set(BIRTHDAY_COLUMN, "?")
                                           .set(BIRTHDAY_STRING_COLUMN, "?")
                                           .where(ID_COLUMN + " = 0")
                                           .build();
            String parametersJson = """
                    [
                        {
                            "value": 2700
                        },
                        {
                            "value": "testUpdateWithParamsObjectsArray"
                        },
                        {
                            "value": "2007-07-27"
                        },
                        {
                            "value": "20080828"
                        }
                    ]
                    """;
            int updatedRows = DatabaseFacade.update(updateSql, parametersJson);
            assertThat(updatedRows).isEqualTo(1);

            Assertions.assertThat(createAssertTestTable())
                      .hasNumberOfRows(1)

                      .row(0)
                      .value(ID_COLUMN)
                      .isEqualTo(2700)
                      .value(NAME_COLUMN)
                      .isEqualTo("testUpdateWithParamsObjectsArray")
                      .value(BIRTHDAY_COLUMN)
                      .isEqualTo(Date.valueOf("2007-07-27"))
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isEqualTo("20080828");
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
        void testNextval() throws Throwable {
            String seqName = "TEST_SEQ_05";
            DatabaseFacade.createSequence(seqName, 1);

            long nextValue = DatabaseFacade.nextval(seqName);
            assertThat(nextValue).isEqualTo(1);

            nextValue = DatabaseFacade.nextval(seqName);
            assertThat(nextValue).isEqualTo(2);
        }

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
                DatabaseFacade.createSequence("TEST_SEQ_04");
                DatabaseFacade.dropSequence("TEST_SEQ_04");
            });
        }

        @Test
        void testDropSequenceByNameAndDataSourceName() {
            assertDoesNotThrow(() -> {
                DatabaseFacade.createSequence("TEST_SEQ_05", 300, systemDataSource);
                DatabaseFacade.dropSequence("TEST_SEQ_05", systemDataSource);
            });
        }
    }


    @Nested
    class InsertTest {

        @Test
        void testInsertInTableWithAllSupportedColumnTypes() throws Throwable {
            DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
            ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
            String tableName = "ALL_COLUMN_TYPES_TABLE";
            String createTableSql = dialect.create()
                                           .table(tableName)
                                           .columnInteger("INT_COLUMN")
                                           .columnVarchar("VARCHAR_COLUMN", 50)
                                           .columnDate("DATE_COLUMN")
                                           .columnTime("TIME_COLUMN")
                                           .columnTimestamp("TIMESTAMP_COLUMN")
                                           .columnBoolean("BOOLEAN_COLUMN")
                                           .columnSmallint("SMALLINT_COLUMN")
                                           .columnReal("REAL_COLUMN")
                                           .columnDouble("DOUBLE_COLUMN")
                                           .columnDecimal("DECIMAL_COLUMN", 17, 2)
                                           .columnBigint("BIGINT_COLUMN")
                                           .build();
            try (DirigibleConnection connection = defaultDataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(createTableSql)) {
                preparedStatement.executeUpdate();
            }

            String insertSql = getDialect().insert()
                                           .into(tableName)
                                           .column("INT_COLUMN")
                                           .column("VARCHAR_COLUMN")
                                           .column("DATE_COLUMN")
                                           .column("TIME_COLUMN")
                                           .column("TIMESTAMP_COLUMN")
                                           .column("BOOLEAN_COLUMN")
                                           .column("SMALLINT_COLUMN")
                                           .column("REAL_COLUMN")
                                           .column("DOUBLE_COLUMN")
                                           .column("DECIMAL_COLUMN")
                                           .column("BIGINT_COLUMN")
                                           .build();
            String parametersJson = """
                    [
                        42,
                        "example text",
                        "2025-05-20",
                        "14:30:00",
                        "2025-05-20 14:30:00",
                        true,
                        32000,
                        3.14,
                        2.7182818284,
                        123456789012345.67,
                        9223372036854775807
                    ]
                    """;
            DatabaseFacade.insert(insertSql, parametersJson, null);

            Table table = dbAsserter.getDefaultDbTable(tableName);
            Assertions.assertThat(table)
                      .hasNumberOfRows(1)

                      .row(0)
                      .value("INT_COLUMN")
                      .isEqualTo(42)

                      .value("VARCHAR_COLUMN")
                      .isEqualTo("example text")

                      .value("DATE_COLUMN")
                      .isEqualTo(Date.valueOf("2025-05-20"))

                      .value("TIME_COLUMN")
                      .isEqualTo(Time.valueOf("14:30:00"))

                      .value("TIMESTAMP_COLUMN")
                      .isEqualTo(Timestamp.valueOf("2025-05-20 14:30:00"))

                      .value("BOOLEAN_COLUMN")
                      .isEqualTo(true)

                      .value("SMALLINT_COLUMN")
                      .isEqualTo(32000)

                      .value("REAL_COLUMN")
                      .isEqualTo(Double.valueOf(3.14))

                      .value("DOUBLE_COLUMN")
                      .isEqualTo(2.7182818284)

                      .value("DECIMAL_COLUMN")
                      .isEqualTo(123456789012345.67)

                      .value("BIGINT_COLUMN")
                      .isEqualTo(new BigInteger("9223372036854775807"));
        }

        @Test
        void testInsertWithParamsArrayWithNulls() throws Throwable {
            Object[] params = {1, null, null, null};
            String insertSql = createInsertInTestTableAllColumnsSql();

            DatabaseFacade.insert(insertSql, createParamsJson(params), null);

            Table table = createAssertTestTable();
            Assertions.assertThat(table)
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull();
        }

        // Not valid anymore
        // @Test
        // void testInsertWithInvalidParamsObjects() throws Throwable {
        // String parametersJson = "[[],[],[],[]]";
        //
        // String insertSql = createInsertInTestTableAllColumnsSql();
        //
        // Exception thrownException =
        // assertThrows(JdbcSQLDataException.class, () -> DatabaseFacade.insert(insertSql, parametersJson,
        // null));
        //
        // assertThat(thrownException.getMessage()).contains(
        // "Parameter with index [1] must be primitive or object. Parameter element [[]]");
        //
        // }

        @Test
        void testInsertWithParamsObjectsArrayWithNulls() throws Throwable {
            String parametersJson = """
                    [
                        {
                            "value": 1
                        },
                        {
                            "value": null
                        },
                        {
                            "value": null
                        },
                        {
                            "value": null
                        }
                    ]
                    """;

            String insertSql = createInsertInTestTableAllColumnsSql();
            DatabaseFacade.insert(insertSql, parametersJson, null);

            Table table = createAssertTestTable();
            Assertions.assertThat(table)
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull();
        }

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
            String insertSql = createInsertInTestTableAllColumnsSql();
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
            String insertSql = createInsertInTestTableAllColumnsSql();
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
        void testInsertNamedWithInvalidParamsObjects() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .value(":id")
                                           .build();
            String parametersJson = "[[]]";
            Exception thrownException =
                    assertThrows(IllegalArgumentException.class, () -> DatabaseFacade.insertNamed(insertSql, parametersJson, null));

            assertThat(thrownException.getMessage()).contains("Parameters must contain objects only. Parameter element [[]]");
        }

        @Test
        void testInsertWithInvalidParametersJson() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .value("1")
                                           .build();
            String parametersJson = "[{]";
            Exception thrownException =
                    assertThrows(IllegalArgumentException.class, () -> DatabaseFacade.insert(insertSql, parametersJson, null));

            assertThat(thrownException.getMessage()).contains("Invalid json: " + parametersJson);
        }

        @Test
        void testInsertNamedWithInvalidParametersJson() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .value(":name")
                                           .build();
            String parametersJson = "[{]";
            Exception thrownException =
                    assertThrows(IllegalArgumentException.class, () -> DatabaseFacade.insertNamed(insertSql, parametersJson, null));

            assertThat(thrownException.getMessage()).contains("Invalid json: " + parametersJson);
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
        void testQueryWithInvalidResultParametersJson() throws Throwable {
            String selectQuery = getDialect().select()
                                             .from(TEST_TABLE)
                                             .build();
            String resultParametersJson = "{[}";

            Exception thrownException =
                    assertThrows(IllegalArgumentException.class, () -> DatabaseFacade.query(selectQuery, null, null, resultParametersJson));

            assertThat(thrownException.getMessage()).contains("Json: {[}] cannot be deserialized to ");
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
        void testInsertManyWithParamsObjectsArray() throws Throwable {

            String parametersJson = """
                    [
                        [
                            {
                                "value": 1
                            },
                            {
                                "value": "John"
                            },
                            {
                                "value": "2000-12-20"
                            },
                            {
                                "value": "20001121"
                            }
                        ],
                        [
                            {
                                "value": 2
                            },
                            {
                                "value": "Mary"
                            },
                            {
                                "value": "2001-11-21"
                            },
                            {
                                "value": "20001222"
                            }
                        ]
                    ]
                    """;

            insertMany(parametersJson);

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

        private void insertMany(String parametersJson) throws Throwable {
            String insertSql = createInsertInTestTableAllColumnsSql();
            DatabaseFacade.insertMany(insertSql, parametersJson, null);
        }

        @Test
        void testInsertManyWithParamsArrayWithNulls() throws Throwable {
            Object[][] params = {//
                    {1, null, null, null}, //
                    {2, null, null, null}//
            };
            insertMany(params);

            Table table = createAssertTestTable();
            Assertions.assertThat(table)
                      .hasNumberOfRows(3)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull()

                      .row(2)
                      .value(ID_COLUMN)
                      .isEqualTo(2)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull();
        }

        private void insertMany(Object[][] params) throws Throwable {
            String parametersJson = createMultiParamsJson(params);
            insertMany(parametersJson);
        }

        @Test
        void testInsertManyWithParamsObjectsArrayWithNulls() throws Throwable {
            String parametersJson = """
                    [
                        [
                            {
                                "value": 1
                            },
                            {
                                "value": null
                            },
                            {
                                "value": null
                            },
                            {
                                "value": null
                            }
                        ],
                        [
                            {
                                "value": 2
                            },
                            {
                                "value": null
                            },
                            {
                                "value": null
                            },
                            {
                            }
                        ]
                    ]
                    """;
            insertMany(parametersJson);

            Table table = createAssertTestTable();
            Assertions.assertThat(table)
                      .hasNumberOfRows(3)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(1)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull()

                      .row(2)
                      .value(ID_COLUMN)
                      .isEqualTo(2)
                      .value(NAME_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_COLUMN)
                      .isNull()
                      .value(BIRTHDAY_STRING_COLUMN)
                      .isNull();
        }

        @Test
        void testInsertManyWithParamsArray() throws Throwable {
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

        @Test
        void testInsertManyNoParams() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .value("789")
                                           .build();
            DatabaseFacade.insertMany(insertSql, null, null);

            Table table = createAssertTestTable();
            Assertions.assertThat(table)
                      .hasNumberOfRows(2)

                      .row(1)
                      .value(ID_COLUMN)
                      .isEqualTo(789);
        }

        @Test
        void testInsertManyParamsCountMismatch() throws Throwable {
            String insertSql = getDialect().insert()
                                           .into(TEST_TABLE)
                                           .column(ID_COLUMN)
                                           .build();

            Object[][] params = {//
                    {1, "Test"}, //
                    {2, "Test2"}, //
                    {3, "Test3"}//
            };
            String paramsJson = createParamsJson(params);

            Exception thrownException =
                    assertThrows(IllegalArgumentException.class, () -> DatabaseFacade.insertMany(insertSql, paramsJson, null));

            assertThat(thrownException.getMessage()).contains("Provided invalid parameters count of [2]. Expected parameters count [1]");
        }
    }

    private String createInsertInTestTableAllColumnsSql() throws SQLException {
        return getDialect().insert()
                           .into(TEST_TABLE)
                           .column(ID_COLUMN)
                           .column(NAME_COLUMN)
                           .column(BIRTHDAY_COLUMN)
                           .column(BIRTHDAY_STRING_COLUMN)
                           .build();
    }

    private ISqlDialect getDialect() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();

        return SqlDialectFactory.getDialect(dataSource);
    }

    @Test
    void testUpdateNamedWithParamsArray() throws Throwable {
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

    private static void assertPreparedResult(int id, String result) {
        String expectedResult = "[{\"Id\":" + id + ",\"Name\":\"Peter\",\"Birthday\":\"2025-01-20\",\"BirthdayString\":\"2024-02-22\"}]";
        JsonAsserter.assertEquals(expectedResult, result);
    }

    private String queryTestTable() throws Throwable {
        String selectQuery = getDialect().select()
                                         .from(TEST_TABLE)
                                         .build();
        return DatabaseFacade.query(selectQuery);
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
            if (sqlFactory.existsTable(connection, tableName)) {
                String dropSql = sqlFactory.drop()
                                           .table(tableName)
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

    private Table createAssertTestTable() {
        return dbAsserter.getDefaultDbTable(TEST_TABLE);
    }

    private static String createParamsJson(Object... params) {
        return GsonHelper.toJson(params);
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
