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

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.integration.tests.ui.CamelTestProject;
import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ApacheCamelIT extends UserInterfaceIntegrationTest {

    @Autowired
    private CamelTestProject testProject;
    private LogsAsserter consoleLogAsserter;
    private LogsAsserter openCartLogAsserter;

    public static final String EXPECTED_JDBC_START_MESSAGE = "Replicating orders from OpenCart using JDBC...";
    public static final String EXPECTED_JDBC_SUCCESS_MESSAGE = "Successfully replicated orders from OpenCart using JDBC";

    public static final String EXPECTED_TYPESCRIPT_START_MESSAGE = "Replicating orders from OpenCart using TypeScript";
    public static final String EXPECTED_TYPESCRIPT_SUCCESS_MESSAGE = "Successfully replicated orders from OpenCart using TypeScript";
    public static final String EXPECTED_UPSERT_ORDER_1_START = "About to upsert Open cart order [1] using exchange rate";
    public static final String EXPECTED_UPSERT_ORDER_1_SUCCESS = "Upserted Open cart order [1]";
    public static final String EXPECTED_UPSERT_ORDER_2_START = "About to upsert Open cart order [2] using exchange rate";
    public static final String EXPECTED_UPSERT_ORDER_2_SUCCESS = "Upserted Open cart order [2]";

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
        this.openCartLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);

        testProject.publishCamel();
        browser.clearCookies();
    }

    @Test
    public void testImplementETLUsingJDBC() {
        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_JDBC_START_MESSAGE, Level.INFO));
        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_JDBC_SUCCESS_MESSAGE, Level.INFO));

        // CHECK DATABASE IF THE DATA IS REPLICATED
        assertOrdersTablePopulated();
    }

    @Test
    public void testImplementETLUsingTypescript() {
        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_TYPESCRIPT_START_MESSAGE, Level.INFO));

        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_1_START, Level.INFO));

        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_1_SUCCESS, Level.INFO));

        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_2_START, Level.INFO));

        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_2_SUCCESS, Level.INFO));

        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_TYPESCRIPT_SUCCESS_MESSAGE, Level.INFO));

        // CHECK DATABASE IF THE DATA IS REPLICATED
        assertOrdersTablePopulated();
    }

    @AfterEach
    public void tearDown() {
        browser.clearCookies();
    }

    private void assertOrdersTablePopulated() {
        String url;
        String user;
        String password;

        try (Connection testConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres")) {
            url = "jdbc:postgresql://localhost:5432/postgres";
            user = "postgres";
            password = "postgres";
        } catch (SQLException e) {
            // If PostgreSQL is unavailable, fall back to H2
            url = "jdbc:h2:file:./target/dirigible/h2/DefaultDB";
            user = "sa";
            password = "sa";
        }

        try (Connection connection = DriverManager.getConnection(url, user, password);
                PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM \"ORDERS\"");
                ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            long count = resultSet.getLong(1);

            assertThat(count)
                    .as("ORDERS table should have at least one record after ETL execution")
                    .isGreaterThan(0);

        } catch (SQLException e) {
            throw new RuntimeException("Database check for ORDERS table failed", e);
        }
    }

}

