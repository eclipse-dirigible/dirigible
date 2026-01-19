/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Lazy
@Component
class CsvimTestProject extends BaseTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvimTestProject.class);

    private static final String UNDEFINED_TABLE_NAME = "TEST_TABLE_READERS2";
    private static final List<CsvimTestProject.Reader> CSV_READERS =
            List.of(new CsvimTestProject.Reader(1, "Ivan", "Ivanov"), new CsvimTestProject.Reader(2, "Maria", "Petrova"));

    private final DataSourcesManager dataSourcesManager;

    CsvimTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, DataSourcesManager dataSourcesManager) {
        super("CsvimIT", ide, projectUtil, edmView);
        this.dataSourcesManager = dataSourcesManager;
    }

    private record Reader(int id, String firstName, String lastName) {

        @Override
        public String toString() {
            return "Reader{" + "id=" + id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + '}';
        }
    }

    @Override
    public void configure() {
        copyToWorkspace();
        publish(false);
        getIde().close();
    }

    /**
     * Initially the table READERS2 is not defined. However, the other two tables must be imported. Once
     * the table is created, csvim retry should be able to import data in it as well
     */
    @Override
    public void verify() throws SQLException {
        copyToWorkspace();

        verifyDataInTable("TEST_TABLE_READERS", CSV_READERS);
        assertThat(isTableExists(UNDEFINED_TABLE_NAME)).isFalse();
        verifyDataInTable("TEST_TABLE_READERS3", CSV_READERS);

        createUndefinedTable();

        verifyDataInTable("TEST_TABLE_READERS", CSV_READERS);
        verifyDataInTable(UNDEFINED_TABLE_NAME, CSV_READERS);
        verifyDataInTable("TEST_TABLE_READERS3", CSV_READERS);
    }

    private void createUndefinedTable() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(defaultDataSource);
            String sql = dialect.create()
                                .table(UNDEFINED_TABLE_NAME)
                                .column("READER_ID", DataType.INTEGER, true)
                                .columnVarchar("READER_FIRST_NAME", 50)
                                .columnVarchar("READER_LAST_NAME", 50)
                                .build();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                LOGGER.info("Will create table using [{}]", sql);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create table using sql: " + sql, e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create table " + UNDEFINED_TABLE_NAME, e);
        }
    }

    private void verifyDataInTable(String tableName, List<CsvimTestProject.Reader> expectedReaders) {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> {
                   try {
                       List<CsvimTestProject.Reader> readers = getAllData(tableName);

                       assertThat(readers).hasSize(expectedReaders.size());
                       assertThat(readers).containsExactlyInAnyOrderElementsOf(expectedReaders);
                       return true;
                   } catch (AssertionError | RuntimeException ex) {
                       LOGGER.warn("Failed assert table data in [{}]. Expected data [{}] ", tableName, expectedReaders, ex);
                       return false;
                   }
               });
    }

    private List<CsvimTestProject.Reader> getAllData(String tableName) {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = defaultDataSource.getConnection()) {
            String sql = SqlDialectFactory.getDialect(defaultDataSource)
                                          .select()
                                          .from(tableName)
                                          .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSet resultSet = statement.executeQuery();

                List<CsvimTestProject.Reader> results = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("READER_ID");
                    String firstName = resultSet.getString("READER_FIRST_NAME");
                    String lastName = resultSet.getString("READER_LAST_NAME");
                    results.add(new CsvimTestProject.Reader(id, firstName, lastName));
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
