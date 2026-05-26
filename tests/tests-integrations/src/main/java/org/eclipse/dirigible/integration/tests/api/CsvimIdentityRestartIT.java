/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.util.TestConditionsChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that {@code CsvimProcessor} advances a table's IDENTITY counter past the seeded explicit
 * IDs. The seed CSV inserts rows with PKs 1..5; without the post-load restart, the IDENTITY counter
 * would still point at 1 and the next "generate-me-one" INSERT would collide on PK = 1.
 *
 * <p>
 * Pure HTTP / JDBC — no Selenide, no IDE.
 */
class CsvimIdentityRestartIT extends IntegrationTest {

    private static final String PROJECT = "csvim-identity-restart-it";

    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/tables/book.table";
    private static final String CSV_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/books.csv";
    private static final String CSVIM_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/books.csvim";

    private static final String TABLE_NAME = "CSVIM_IDENT_BOOK";

    private static final String TABLE_SOURCE = """
            {
                "name": "CSVIM_IDENT_BOOK",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "identity": true,
                        "nullable": false,
                        "name": "BOOK_ID"
                    },
                    {
                        "type": "VARCHAR",
                        "length": 40,
                        "nullable": false,
                        "name": "BOOK_TITLE"
                    }
                ]
            }
            """;

    private static final String CSV_SOURCE = """
            BOOK_ID,BOOK_TITLE
            1,Dune
            2,Foundation
            3,Neuromancer
            4,Hyperion
            5,Snow Crash
            """;

    private static final String CSVIM_SOURCE = """
            {
                "files": [
                    {
                        "table": "CSVIM_IDENT_BOOK",
                        "schema": "PUBLIC",
                        "file": "/%s/data/books.csv",
                        "header": true,
                        "useHeaderNames": true,
                        "delimField": ",",
                        "distinguishEmptyFromNull": true,
                        "version": "1.0"
                    }
                ]
            }
            """.formatted(PROJECT);

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private TestConditionsChecker testConditionsChecker;

    @Test
    void identity_counter_is_advanced_past_explicitly_seeded_ids() throws Exception {
        // IDENTITY DDL emission is intentionally suppressed on MSSQL (TableCreateProcessor) to keep
        // existing TS/JS code paths that issue explicit-ID INSERTs working. Without IDENTITY there is
        // no counter to advance, so this contract isn't expressible on MSSQL.
        assumeTrue(testConditionsChecker.isH2OrPostgresDefaultDB(),
                "Skipping: IDENTITY DDL is disabled on MSSQL, so identity-counter advancement is not applicable.");

        write(TABLE_PATH, TABLE_SOURCE);
        write(CSV_PATH, CSV_SOURCE);
        write(CSVIM_PATH, CSVIM_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {

            // Sanity: the 5 seeded rows are in place — MAX(BOOK_ID) == 5.
            try (ResultSet rs = statement.executeQuery("SELECT MAX(\"BOOK_ID\") FROM \"" + TABLE_NAME + "\"")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(5);
            }

            // The actual contract: an INSERT that omits BOOK_ID must get id = 6 (not 1).
            // If the IDENTITY counter wasn't advanced, this throws a PK-violation.
            statement.execute("INSERT INTO \"" + TABLE_NAME + "\" (\"BOOK_TITLE\") VALUES ('Cryptonomicon')",
                    Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                assertThat(keys.getInt(1)).isEqualTo(6);
            }

            // And MAX(BOOK_ID) now equals 6, confirming the row landed.
            try (ResultSet rs = statement.executeQuery("SELECT MAX(\"BOOK_ID\") FROM \"" + TABLE_NAME + "\"")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(6);
            }
        }
    }

    private void write(String path, String source) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
    }

    @AfterEach
    void cleanup() throws Exception {
        for (String path : List.of(CSVIM_PATH, CSV_PATH, TABLE_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
        // Drop the table so reruns start from a clean state — the synchronizer's "keep data" policy
        // means the table itself survives a .table file removal otherwise.
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
