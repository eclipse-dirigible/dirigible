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
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.db.DBAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class DatabaseFacadeIT extends IntegrationTest {

    public static final String ID_COLUMN = "Id";
    public static final String NAME_COLUMN = "Name";
    public static final String BIRTHDAY_COLUMN = "Birthday";
    public static final String BIRTHDAY_STRING_COLUMN = "BirthdayString";
    private static final String TEST_TABLE = "TEACHERS";

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private DBAsserter dbAsserter;

    @BeforeEach
    void setUp() throws SQLException {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();

        ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
        String createTableSql = dialect.create()
                                       .table(TEST_TABLE)
                                       .columnInteger(ID_COLUMN, true)
                                       .columnNvarchar(NAME_COLUMN, 20)
                                       .columnDate(BIRTHDAY_COLUMN)
                                       .columnNvarchar(BIRTHDAY_STRING_COLUMN, 20, false)
                                       .build();
        try (DirigibleConnection connection = defaultDataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(createTableSql)) {
            preparedStatement.executeUpdate();
        }
    }

    @Test
    void testInsertMany() throws Throwable {
        Object[][] params = {//
                {1, "John", "2000-12-20", "20001121"}, //
                {2, "Mary", "2001-11-21", "20001222"}//
        };
        insertMany(params);

        Table table = dbAsserter.getDefaultDbTable(TEST_TABLE);

        Assertions.assertThat(table)
                  .hasNumberOfRows(2)

                  .row(0)
                  .value(ID_COLUMN)
                  .isEqualTo(1)
                  .value(NAME_COLUMN)
                  .isEqualTo("John")
                  .value(BIRTHDAY_COLUMN)
                  .isEqualTo(java.sql.Date.valueOf("2000-12-20"))
                  .value(BIRTHDAY_STRING_COLUMN)
                  .isEqualTo("2000-11-21")

                  .row(1)
                  .value(ID_COLUMN)
                  .isEqualTo(2)
                  .value(NAME_COLUMN)
                  .isEqualTo("Mary")
                  .value(BIRTHDAY_COLUMN)
                  .isEqualTo(java.sql.Date.valueOf("2001-11-21"))
                  .value(BIRTHDAY_STRING_COLUMN)
                  .isEqualTo("2000-12-22");
    }

    private void insertMany(Object[][] params) throws Throwable {
        ISqlDialect dialect = getDialect();
        String insertSql = dialect.insert()
                                  .into(TEST_TABLE)
                                  .column(ID_COLUMN)
                                  .column(NAME_COLUMN)
                                  .column(BIRTHDAY_COLUMN)
                                  .column(BIRTHDAY_STRING_COLUMN)
                                  .build();
        String parametersJson = GsonHelper.toJson(params);
        DatabaseFacade.insertMany(insertSql, parametersJson, null, null);

    }

    private ISqlDialect getDialect() throws SQLException {
        DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();

        return SqlDialectFactory.getDialect(dataSource);
    }

}
