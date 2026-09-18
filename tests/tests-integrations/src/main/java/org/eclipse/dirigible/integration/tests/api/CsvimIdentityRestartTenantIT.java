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
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.util.TestConditionsChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The tenant-schema half of {@link CsvimIdentityRestartIT}: the same contract, in a schema the
 * platform named itself.
 *
 * <p>
 * A tenant's schema is its id uppercased and that id is a random UUID, so the name carries hyphens
 * and often a leading digit. The post-load IDENTITY restart used to be gated on an allow-list of
 * bare unquoted identifiers, which such a name can never match, so for every tenant the platform
 * provisions the counter was left where the seed import found it - and the first record a user
 * created collided with a seeded row, once per seeded row, healing itself afterwards and reading as
 * flakiness. The default tenant's {@code PUBLIC} matched the allow-list, which is why the sibling
 * test never saw it.
 *
 * <p>
 * Pure HTTP / JDBC - no Selenide, no IDE.
 */
class CsvimIdentityRestartTenantIT extends IntegrationTest {

    private static final String PROJECT = "csvim-identity-restart-tenant-it";

    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/tables/book.table";
    private static final String CSV_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/books.csv";
    private static final String CSVIM_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/data/books.csvim";

    private static final String TABLE_NAME = "CSVIM_IDENT_TENANT_BOOK";

    private static final String TABLE_SOURCE = """
            {
                "name": "CSVIM_IDENT_TENANT_BOOK",
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

    /**
     * {@code PUBLIC} here means "the schema of the default data source", which inside a tenant scope is
     * that tenant's own schema - the very substitution {@code CsvimProcessor} makes for multitenancy.
     */
    private static final String CSVIM_SOURCE = """
            {
                "files": [
                    {
                        "table": "CSVIM_IDENT_TENANT_BOOK",
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
    private TenantContext tenantContext;

    @Autowired
    private TestConditionsChecker testConditionsChecker;

    @Test
    void identityCounterIsAdvancedInATenantSchemaToo() throws Exception {
        // IDENTITY DDL emission is intentionally suppressed on MSSQL (TableCreateProcessor) to keep
        // existing TS/JS code paths that issue explicit-ID INSERTs working. Without IDENTITY there is
        // no counter to advance, so this contract isn't expressible on MSSQL.
        assumeTrue(testConditionsChecker.isH2OrPostgresDefaultDB(),
                "Skipping: IDENTITY DDL is disabled on MSSQL, so identity-counter advancement is not applicable.");

        DirigibleTestTenant tenant = new DirigibleTestTenant(PROJECT);
        createTenants(tenant);
        waitForTenantProvisioning(tenant);

        write(TABLE_PATH, TABLE_SOURCE);
        write(CSV_PATH, CSV_SOURCE);
        write(CSVIM_PATH, CSVIM_SOURCE);
        synchronizationProcessor.forceProcessSynchronizers();

        tenantContext.execute(tenant.getId(), () -> {
            assertSeededTenantTableAcceptsAGeneratedId();
            return null;
        });
    }

    private void assertSeededTenantTableAcceptsAGeneratedId() throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {

            // The premise of the whole test: this schema is NOT a bare unquoted identifier, so the
            // statements the restart emits have to quote it.
            assertThat(connection.getSchema()).matches(".*[^A-Za-z0-9_].*");

            // Sanity: the 5 seeded rows landed in the tenant's own schema - MAX(BOOK_ID) == 5.
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
        }
    }

    private void write(String path, String source) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
    }
}
