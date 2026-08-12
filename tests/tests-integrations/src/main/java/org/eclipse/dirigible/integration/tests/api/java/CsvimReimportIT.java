/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Re-importing a CSV whose value changed must reach the row, including when the CSV is narrower
 * than its table - the shape of every seed CSV for a table carrying audit columns.
 *
 * <p>
 * The assertions read the row's value rather than the statement: an equally wide CSV binds
 * correctly by accident, so a fixture with as many fields as the table has columns passes even when
 * the index arithmetic is wrong.
 */
class CsvimReimportIT extends IntegrationTest {

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
     * A changed value in a CSV that carries fewer columns than its table must be applied on re-import.
     *
     * @throws Exception the exception
     */
    @Test
    void reimportAppliesChangedValueWhenCsvIsNarrowerThanTable() throws Exception {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_REIMPORT (R1 INT PRIMARY KEY, R2 VARCHAR(20), R3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(false);
                CsvFile csvFile = new CsvFile(null, "CSV_REIMPORT", null, "import", true, true, ",", "\"", null, false, null);

                csvimProcessor.process(csvFile, "R1,R2,R3\n1,r2_1,r3_1\n2,r2_2,r3_2".getBytes(), defaultDataSourceName);

                // the same rows with one changed value, in a CSV one column narrower than the table
                csvimProcessor.process(csvFile, "R1,R2\n1,r2_1_changed\n2,r2_2".getBytes(), defaultDataSourceName);

                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT R2, R3 FROM CSV_REIMPORT WHERE R1 = 1");
                assertTrue(rs.next(), "Row with R1 = 1 is missing after the re-import");
                assertEquals("r2_1_changed", rs.getString("R2"), "The changed value did not reach the row");
                assertNull(rs.getString("R3"), "A column the CSV does not carry must be set to null, not to the record's id");

                rs = connection.createStatement()
                               .executeQuery("SELECT COUNT(*) FROM CSV_REIMPORT");
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "The re-import must update the existing rows, not add new ones");
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_REIMPORT");
            }
        }
    }

    /**
     * The same for a headerless CSV, where values are matched to columns by position.
     *
     * @throws Exception the exception
     */
    @Test
    void reimportAppliesChangedValueWhenHeaderlessCsvIsNarrowerThanTable() throws Exception {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection()) {
            connection.createStatement()
                      .execute("CREATE TABLE CSV_REIMPORT_NH (N1 INT PRIMARY KEY, N2 VARCHAR(20), N3 VARCHAR(20))");
            try {
                csvimProcessor.setStrictMode(false);
                CsvFile csvFile = new CsvFile(null, "CSV_REIMPORT_NH", null, "import", false, false, ",", "\"", null, false, null);

                csvimProcessor.process(csvFile, "1,n2_1\n2,n2_2".getBytes(), defaultDataSourceName);

                csvimProcessor.process(csvFile, "1,n2_1_changed\n2,n2_2".getBytes(), defaultDataSourceName);

                ResultSet rs = connection.createStatement()
                                         .executeQuery("SELECT N2, N3 FROM CSV_REIMPORT_NH WHERE N1 = 1");
                assertTrue(rs.next(), "Row with N1 = 1 is missing after the re-import");
                assertEquals("n2_1_changed", rs.getString("N2"), "The changed value did not reach the row");
                assertNull(rs.getString("N3"), "A column the record does not reach must be bound as null");

                rs = connection.createStatement()
                               .executeQuery("SELECT COUNT(*) FROM CSV_REIMPORT_NH");
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "The re-import must update the existing rows, not add new ones");
            } finally {
                connection.createStatement()
                          .execute("DROP TABLE CSV_REIMPORT_NH");
            }
        }
    }
}
