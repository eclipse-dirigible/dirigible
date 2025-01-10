/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.integration.tests.ui.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class CsvimIT extends UserInterfaceIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvimIT.class);

    private static final String UNDEFINIED_TABLE_NAME = "TEST_TABLE_READERS2";
    private static final String TEST_PROJECT_FOLDER_PATH = "CsvimIT/csvim-test-project";
    private static final List<Reader> CSV_READERS = List.of(new Reader(1, "Ivan", "Ivanov"), new Reader(2, "Maria", "Petrova"));

    @Autowired
    private DataSourcesManager dataSourcesManager;


    private static class Reader {
        private final int id;
        private final String firstName;
        private final String lastName;

        Reader(int id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Reader reader = (Reader) o;
            return id == reader.id && Objects.equals(firstName, reader.firstName) && Objects.equals(lastName, reader.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, firstName, lastName);
        }

        @Override
        public String toString() {
            return "Reader{" + "id=" + id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + '}';
        }
    }

    /**
     * Initially the table READERS2 is not defined. However, the other two tables must be imported. Once
     * the table is created, csvim retry should be able to import data in it as well
     */
    @Test
    void testImportData() throws SQLException {
        ide.createAndPublishProjectFromResources(TEST_PROJECT_FOLDER_PATH);

        verifyDataInTable("TEST_TABLE_READERS", CSV_READERS);
        assertThat(isTableExists(UNDEFINIED_TABLE_NAME)).isFalse();
        verifyDataInTable("TEST_TABLE_READERS3", CSV_READERS);

        createUndefinedTable();

        verifyDataInTable("TEST_TABLE_READERS", CSV_READERS);
        verifyDataInTable(UNDEFINIED_TABLE_NAME, CSV_READERS);
        verifyDataInTable("TEST_TABLE_READERS3", CSV_READERS);

    }

    private void createUndefinedTable() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
            String sql = dialect.create()
                                .table(UNDEFINIED_TABLE_NAME)
                                .column("READER_ID", DataType.INTEGER, true)
                                .column("READER_FIRST_NAME", DataType.VARCHAR)
                                .column("READER_LAST_NAME", DataType.VARCHAR)
                                .build();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                LOGGER.info("Will create table using [{}]", sql);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create table using sql: " + sql, e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create table " + UNDEFINIED_TABLE_NAME, e);
        }
    }

    private void verifyDataInTable(String tableName, List<Reader> expectedReaders) {
        await().atMost(35, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> {
                   try {
                       List<Reader> readers = getAllData(tableName);

                       assertThat(readers).hasSize(expectedReaders.size());
                       assertThat(readers).containsExactlyInAnyOrderElementsOf(expectedReaders);
                       return true;
                   } catch (AssertionError | RuntimeException ex) {
                       LOGGER.warn("Failed assert table data in [{}]. Expected data [{}] ", tableName, expectedReaders, ex);
                       return false;
                   }
               });
    }

    private List<Reader> getAllData(String tableName) {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            String sql = SqlDialectFactory.getDialect(defaultDataSource)
                                          .select()
                                          .from(tableName)
                                          .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSet resultSet = statement.executeQuery();

                List<Reader> results = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("READER_ID");
                    String firstName = resultSet.getString("READER_FIRST_NAME");
                    String lastName = resultSet.getString("READER_LAST_NAME");
                    results.add(new Reader(id, firstName, lastName));
                }
                return results;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get all data from " + tableName, ex);
        }
    }

    private boolean isTableExists(String tableName) throws SQLException {
        DataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            return SqlFactory.getNative(connection)
                             .existsTable(connection, tableName);
        }
    }

}
