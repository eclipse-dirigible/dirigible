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
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

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
 *
 * <p>
 * Also covers the other half of the same finding: the re-import must be TRIGGERED when only the
 * referenced CSV changes, since the .csvim itself is a stable pointer that stays byte-identical.
 */
class CsvimReimportIT extends IntegrationTest {

    /** The project holding the synchronizer-path fixture. */
    private static final String PROJECT = "csvim-csv-edit-it";

    /** The table fixture path. */
    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/tables/city.table";

    /** The CSV fixture path. */
    private static final String CSV_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/cities.csv";

    /** The CSVIM fixture path. */
    private static final String CSVIM_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/cities.csvim";

    /** The table fixture name. */
    private static final String TABLE_NAME = "CSVIM_CSV_EDIT";

    /** The table fixture source. */
    private static final String TABLE_SOURCE = """
            {
                "name": "CSVIM_CSV_EDIT",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "nullable": false,
                        "name": "CITY_ID"
                    },
                    {
                        "type": "VARCHAR",
                        "length": 40,
                        "nullable": false,
                        "name": "CITY_NAME"
                    }
                ]
            }
            """;

    /** The CSVIM fixture source - a stable pointer that is NEVER touched after the initial import. */
    private static final String CSVIM_SOURCE = """
            {
                "files": [
                    {
                        "table": "CSVIM_CSV_EDIT",
                        "schema": "PUBLIC",
                        "file": "/%s/data/cities.csv",
                        "header": true,
                        "useHeaderNames": true,
                        "delimField": ",",
                        "distinguishEmptyFromNull": true,
                        "version": "1.0"
                    }
                ]
            }
            """.formatted(PROJECT);

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

    /** The repository. */
    @Autowired
    private IRepository repository;

    /** The synchronization processor. */
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /**
     * Editing ONLY the referenced CSV must re-import - the .csvim is a stable pointer and does not
     * change when a seed value does, so the change detection has to span the referenced files. The
     * assertion reads the row's value after the second sync; a fixture that also touched the .csvim
     * would pass while missing the bug entirely.
     *
     * @throws Exception the exception
     */
    @Test
    void editingOnlyTheCsvTriggersReimport() throws Exception {
        write(TABLE_PATH, TABLE_SOURCE);
        write(CSV_PATH, "CITY_ID,CITY_NAME\n1,Sofia\n2,Plovdiv");
        write(CSVIM_PATH, CSVIM_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        assertEquals("Sofia", cityName(1), "The initial import did not seed the row");

        // the ordinary editing path: the seed value changes, the .csvim stays byte-identical
        write(CSV_PATH, "CITY_ID,CITY_NAME\n1,Varna\n2,Plovdiv");
        synchronizationProcessor.forceProcessSynchronizers();

        assertEquals("Varna", cityName(1), "Editing only the CSV must re-import the changed value");
    }

    /**
     * Reads the seeded row's name.
     *
     * @param id the city id
     * @return the city name
     * @throws Exception the exception
     */
    private String cityName(int id) throws Exception {
        try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                      .getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT \"CITY_NAME\" FROM \"" + TABLE_NAME + "\" WHERE \"CITY_ID\" = " + id)) {
            assertTrue(rs.next(), "Row with CITY_ID = " + id + " is missing");
            return rs.getString(1);
        }
    }

    /**
     * Writes a fixture resource.
     *
     * @param path the path
     * @param source the source
     */
    private void write(String path, String source) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
    }

    /**
     * Cleanup the fixture.
     *
     * @throws Exception the exception
     */
    @AfterEach
    void cleanup() throws Exception {
        boolean fixtureUsed = false;
        for (String path : List.of(CSVIM_PATH, CSV_PATH, TABLE_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                fixtureUsed = true;
            }
        }
        if (fixtureUsed) {
            synchronizationProcessor.forceProcessSynchronizers();
            try (Connection connection = dataSourceManager.getDefaultDataSource()
                                                          .getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
            }
        }
    }

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
