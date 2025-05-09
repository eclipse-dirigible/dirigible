/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java;

import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.csvim.processor.CsvimProcessor;
import org.eclipse.dirigible.components.data.sources.config.DefaultDataSourceName;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CsvProcessorTest extends IntegrationTest {

    /** The default data source name. */
    @Autowired
    @DefaultDataSourceName
    private String defaultDataSourceName;

    /** The data source manager. */
    @Autowired
    private DataSourcesManager dataSourceManager;

    /** The csvim processor. */
    @Autowired
    private CsvimProcessor csvimProcessor;

    /**
     * Import strict.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void importStrict() throws SQLException {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_A (A1 INT PRIMARY KEY, A2 VARCHAR(20), A3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(true);
                byte[] content = "A1,A2,A3\n1,a2_1,a3_1\n2,a2_2,a3_2".getBytes();
                CsvFile csvFile = new CsvFile(null, "CSV_A", null, "import", true, true, ",", "\"", null, false, null);
                csvimProcessor.process(csvFile, content, defaultDataSourceName);
                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT COUNT(*) FROM CSV_A");
                if (rs.next()) {
                    int c = rs.getInt(1);
                    assertEquals(2, c, "No data has been imported from CSV file CSV_A.csv");
                } else {
                    fail("No data has been imported from CSV file CSV_A.csv");
                }
            } catch (Exception e) {
                fail(e.getMessage(), e);
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_A");
            }

        }
    }

    /**
     * Import strict negative.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void importStrictNegative() throws SQLException {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_A (A1 INT PRIMARY KEY, A2 VARCHAR(20), A3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(true);
                byte[] content = "A1,A2\n1,a2_1\n2,a2_2".getBytes();
                CsvFile csvFile = new CsvFile(null, "CSV_A", null, "import", true, true, ",", "\"", null, false, null);
                try {
                    csvimProcessor.process(csvFile, content, defaultDataSourceName);
                } catch (Exception e) {
                    //
                }
                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT COUNT(*) FROM CSV_A");
                if (rs.next()) {
                    int c = rs.getInt(1);
                    assertEquals(0, c, "Data has been imported from CSV file CSV_A.csv in a strict mode and wrong CSV file");
                }
            } catch (Exception e) {
                fail(e.getMessage(), e);
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_A");
            }

        }
    }

    /**
     * Import strict negative.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void importNonStrictMode() throws SQLException {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_A (A1 INT PRIMARY KEY, A2 VARCHAR(20), A3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(false);
                byte[] content = "A1,A2\n1,a2_1\n2,a2_2".getBytes();
                CsvFile csvFile = new CsvFile(null, "CSV_A", null, "import", true, true, ",", "\"", null, false, null);
                try {
                    csvimProcessor.process(csvFile, content, defaultDataSourceName);
                } catch (Exception e) {
                    //
                }
                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT COUNT(*) FROM CSV_A");
                if (rs.next()) {
                    int c = rs.getInt(1);
                    assertEquals(2, c, "No data has been imported from CSV file CSV_A.csv");
                } else {
                    fail("No data has been imported from CSV file CSV_A.csv");
                }
            } catch (Exception e) {
                fail(e.getMessage(), e);
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_A");
            }

        }
    }

    /**
     * Import strict negative.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void importNonStrictModeScrumbled() throws SQLException {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_A (A1 INT PRIMARY KEY, A2 VARCHAR(20), A3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(false);
                byte[] content = "A1,A3,A2,A4\n1,a3_1,a2_1,a4_1\n2,a3_2,a2_3,a4_1".getBytes();
                CsvFile csvFile = new CsvFile(null, "CSV_A", null, "import", true, true, ",", "\"", null, false, null);
                try {
                    csvimProcessor.process(csvFile, content, defaultDataSourceName);
                } catch (Exception e) {
                    //
                }
                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT COUNT(*) FROM CSV_A");
                if (rs.next()) {
                    int c = rs.getInt(1);
                    assertEquals(2, c, "No data has been imported from CSV file CSV_A.csv");

                    rs = connection.createStatement()
                                   .executeQuery("SELECT * FROM CSV_A");
                    if (rs.next()) {
                        assertEquals("a2_1", rs.getString("A2"));
                        assertEquals("a3_1", rs.getString("A3"));
                    }
                } else {
                    fail("No data has been imported from CSV file CSV_A.csv");
                }
            } catch (Exception e) {
                fail(e.getMessage(), e);
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_A");
            }

        }
    }
}
