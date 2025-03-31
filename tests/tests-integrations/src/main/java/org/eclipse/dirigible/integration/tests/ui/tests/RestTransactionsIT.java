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
import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.DirigibleTestTenant;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@Import(RestTransactionsITConfig.class)
public class RestTransactionsIT extends UserInterfaceIntegrationTest {

    // static {
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_DRIVER", "org.postgresql.Driver");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_URL",
    // "jdbc:postgresql://localhost:5432/postgres");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_USERNAME", "postgres");
    // Configuration.set("DIRIGIBLE_DATASOURCE_DEFAULT_PASSWORD", "postgres");
    // }

    @Autowired
    private UserService userService;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void testCommitByDefaultForSystemDb() {
        given().get(RestTransactionsITConfig.TestRest.COMMIT_BY_DEFAULT_FOR_SYSTEM_DB_PATH)
               .then()
               .statusCode(200)
               .body(containsString("Done"));

        Optional<User> createdUser = userService.findUserByUsernameAndTenantId(RestTransactionsITConfig.TestRest.TEST_USERNAME,
                DirigibleTestTenant.createDefaultTenant()
                                   .getId());
        assertThat(createdUser).isNotEmpty();
    }

    @Test
    void testTransactionalAnnotationWorksForSystemDB() {
        given().get(RestTransactionsITConfig.TestRest.TRANSACTIONAL_ANNOTATION_SYSTEM_DB_TEST_PATH)
               .then()
               .statusCode(500);

        Optional<User> createdUser = userService.findUserByUsernameAndTenantId(RestTransactionsITConfig.TestRest.TEST_USERNAME,
                DirigibleTestTenant.createDefaultTenant()
                                   .getId());
        assertThat(createdUser).isEmpty();
    }

    @Disabled("Disabled until transaction logic is implemented")
    @Test
    void testProgrammaticTransactionRollbackForDefaultDb() throws SQLException {
        createTestTable(dataSourcesManager.getDefaultDataSource());

        given().get(RestTransactionsITConfig.TestRest.PROGRAMMATIC_TRANSACTION_ROLLBACK_DEFAULT_DB_PATH)
               .then()
               .statusCode(500);

        assertTestTableSize(0);
    }

    private void createTestTable(DirigibleDataSource dataSource) throws SQLException {
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);

        String sql = dialect.create()
                            .table(RestTransactionsITConfig.TestRest.TEST_TABLE)
                            .column(RestTransactionsITConfig.TestRest.ID_COLUMN, DataType.INTEGER, true)
                            .build();
        try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
    }

    private void assertTestTableSize(int expectedSize) throws SQLException {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            int count = dialect.count(connection, RestTransactionsITConfig.TestRest.TEST_TABLE);
            assertThat(count).isEqualTo(expectedSize);
        }
    }

    @Test
    void testProgrammaticTransactionCommitForDefaultDb() throws SQLException {
        testInsertedEntries(RestTransactionsITConfig.TestRest.PROGRAMMATIC_TRANSACTION_COMMIT_DEFAULT_DB_PATH);
    }

    private void testInsertedEntries(String path) throws SQLException {
        createTestTable(dataSourcesManager.getDefaultDataSource());

        given().get(path)
               .then()
               .statusCode(200)
               .body(containsString("Done"));

        assertTestTableSize(2);
    }

    @Test
    void testCommitByDefaultForDefaultDb() throws SQLException {
        testInsertedEntries(RestTransactionsITConfig.TestRest.COMMIT_BY_DEFAULT_FOR_DEFAULT_DB_PATH);
    }

}
