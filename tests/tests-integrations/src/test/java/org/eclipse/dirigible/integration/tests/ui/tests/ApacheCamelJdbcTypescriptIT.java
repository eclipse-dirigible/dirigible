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
import org.eclipse.dirigible.integration.tests.ui.tests.projects.CamelTestProject;
import org.eclipse.dirigible.tests.DirigibleCleaner;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ApacheCamelJdbcTypescriptIT extends UserInterfaceIntegrationTest {

    @Autowired
    private CamelTestProject testProject;
    private LogsAsserter camelLogAsserter;
    private LogsAsserter openCartLogAsserter;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheCamelJdbcTypescriptIT.class);

    @BeforeEach
    void setUp() {
        this.camelLogAsserter = new LogsAsserter("app.out", Level.INFO);
        this.openCartLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);
    }

    @Test
    void testImplementETLUsingJDBC() {
        testProject.publishJDBC();

        assertLogContainsMessage(openCartLogAsserter, "Replicating orders from OpenCart using JDBC...", Level.INFO);
        assertLogContainsMessage(openCartLogAsserter, "Successfully replicated orders from OpenCart using JDBC", Level.INFO);

        assertDatabaseETLCompletion();
    }

    @Test
    void testImplementETLUsingTypescript() {
        testProject.publishTypescript();

        assertLogContainsMessage(openCartLogAsserter, "Replicating orders from OpenCart using TypeScript", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "About to upsert Open cart order [1] using exchange rate", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Upserted Open cart order [1]", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "About to upsert Open cart order [2] using exchange rate", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Upserted Open cart order [2]", Level.INFO);
        assertLogContainsMessage(openCartLogAsserter, "Successfully replicated orders from OpenCart using TypeScript", Level.INFO);

        assertDatabaseETLCompletion();
    }

    private void assertLogContainsMessage(LogsAsserter logAsserter, String message, Level level) {
        await().atMost(60, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> logAsserter.containsMessage(message, level));
    }

    private void assertDatabaseETLCompletion() {
        String url;
        String user;
        String password;

        try (Connection testConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/testdb", "testuser", "testpass")) {
            url = "jdbc:postgresql://localhost:5432/testdb";
            user = "testuser";
            password = "testpass";
        } catch (SQLException e) {
            // If PostgreSQL is unavailable, fall back to H2
            url = "jdbc:h2:file:./target/dirigible/h2/DefaultDB";
            user = "sa";
            password = "";
        }

        try (Connection connection = DriverManager.getConnection(url, user, password);
                PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM \"ORDERS\"");
                ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            long count = resultSet.getLong(1);

            assertThat(count).as("ORDERS table should have at least one record after ETL execution")
                             .isGreaterThan(0);
        } catch (SQLException e) {
            throw new RuntimeException("Database check for ORDERS table failed", e);
        }
    }
}
