/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.config.TransactionExecutor;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

@TestConfiguration
class RestTransactionsITConfig {

    @RestController
    static class TestRest {

        static final String ID_COLUMN = "id";
        static final String TEST_TABLE = "TESTTABLE";

        static final String TRANSACTIONAL_ANNOTATION_SYSTEM_DB_TEST_PATH =
                "/services/core/version/rest/api/transactions/testTransactionalAnnotationForSystemDb";
        static final String COMMIT_BY_DEFAULT_FOR_SYSTEM_DB_PATH =
                "/services/core/version/rest/api/transactions/testCommitByDefaultForSystemDb";

        static final String PROGRAMMATIC_TRANSACTION_ROLLBACK_DEFAULT_DB_PATH =
                "/services/core/version/rest/api/transactions/testProgrammaticTransactionRollbackForDefaultDb";
        static final String PROGRAMMATIC_TRANSACTION_COMMIT_DEFAULT_DB_PATH =
                "/services/core/version/rest/api/transactions/testProgrammaticTransactionCommitForDefaultDb";
        static final String COMMIT_BY_DEFAULT_FOR_DEFAULT_DB_PATH =
                "/services/core/version/rest/api/transactions/testCommitByDefaultForDefaultDb";

        static final String TEST_USERNAME = "test-user";
        static final String TEST_PASSWORD = "test-password";

        private final UserService userService;
        private final TenantContext tenantContext;
        private final DataSourcesManager dataSourcesManager;

        TestRest(UserService userService, TenantContext tenantContext, DataSourcesManager dataSourcesManager) {
            this.userService = userService;
            this.tenantContext = tenantContext;
            this.dataSourcesManager = dataSourcesManager;
        }

        @Transactional
        @GetMapping(TRANSACTIONAL_ANNOTATION_SYSTEM_DB_TEST_PATH)
        String testTransactionalAnnotationForSystemDb() {
            userService.createNewUser(TEST_USERNAME, TEST_PASSWORD, tenantContext.getCurrentTenant()
                                                                                 .getId());
            throw new IllegalStateException("Intentionally throw an exception to test REST transactional behaviour for system db");
        }

        @GetMapping(COMMIT_BY_DEFAULT_FOR_SYSTEM_DB_PATH)
        String testCommitByDefaultForSystemDb() {
            userService.createNewUser(TEST_USERNAME, TEST_PASSWORD, tenantContext.getCurrentTenant()
                                                                                 .getId());
            return "Done";
        }

        @GetMapping(PROGRAMMATIC_TRANSACTION_ROLLBACK_DEFAULT_DB_PATH)
        String testProgrammaticTransactionRollbackForDefaultDb() {
            DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
            return TransactionExecutor.executeInTransaction(dataSource, () -> {
                ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);

                insertTestRecord(dialect, dataSource);
                throw new IllegalStateException("Intentionally throw an exception to test REST transactional behaviour for default db.");
            });
        }

        private void insertTestRecord(ISqlDialect dialect, DirigibleDataSource dataSource) throws SQLException {
            Random random = new Random();
            int randomId = random.nextInt(10000) + 1;
            String sql = dialect.insert()
                                .into(TestRest.TEST_TABLE)
                                .column(TestRest.ID_COLUMN)
                                .value(Integer.toString(randomId))
                                .generate();
            try (Connection connection = dataSource.getConnection();

                    PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }
        }

        @GetMapping(PROGRAMMATIC_TRANSACTION_COMMIT_DEFAULT_DB_PATH)
        String testProgrammaticTransactionCommitForDefaultDb() {
            DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
            return TransactionExecutor.executeInTransaction(dataSource, () -> insertTwoEntries(dataSource));
        }

        private String insertTwoEntries(DirigibleDataSource dataSource) throws SQLException {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);

            insertTestRecord(dialect, dataSource);
            insertTestRecord(dialect, dataSource);

            return "Done";
        }

        @GetMapping(COMMIT_BY_DEFAULT_FOR_DEFAULT_DB_PATH)
        String testCommitByDefaultForDefaultDb() throws Throwable {
            DirigibleDataSource dataSource = dataSourcesManager.getDefaultDataSource();
            return insertTwoEntries(dataSource);
        }

    }

}
