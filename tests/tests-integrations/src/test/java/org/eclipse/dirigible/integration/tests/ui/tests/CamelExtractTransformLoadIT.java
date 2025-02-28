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
import org.eclipse.dirigible.integration.tests.ui.tests.projects.CamelJDBCTestProject;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.CamelTypescriptTestProject;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// ! REMOVE LOGGER
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;

import org.assertj.db.type.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.assertj.db.api.Assertions;
import static org.awaitility.Awaitility.await;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;

class CamelExtractTransformLoadIT extends UserInterfaceIntegrationTest {

    @Autowired
    private CamelJDBCTestProject JdbcTestProject;
    @Autowired
    private CamelTypescriptTestProject TypescriptTestProject;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    private LogsAsserter camelLogAsserter;
    private LogsAsserter openCartLogAsserter;
    // Remove logger
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelExtractTransformLoadIT.class);

    @BeforeEach
    void setUp() {
        this.camelLogAsserter = new LogsAsserter("app.out", Level.INFO);
        this.openCartLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);
    }

    @Test
    void testImplementETLUsingJDBC() {
        JdbcTestProject.publish();

        assertLogContainsMessage(openCartLogAsserter, "Replicating orders from OpenCart using JDBC...", Level.INFO);
        assertLogContainsMessage(openCartLogAsserter, "Successfully replicated orders from OpenCart using JDBC", Level.INFO);

        assertDatabaseETLCompletion();
    }

    @Test
    void testImplementETLUsingTypescript() {
        TypescriptTestProject.publish();

        assertLogContainsMessage(openCartLogAsserter, "Replicating orders from OpenCart using TypeScript", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "About to upsert Open cart order [1] using exchange rate", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Upserted Open cart order [1]", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "About to upsert Open cart order [2] using exchange rate", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Upserted Open cart order [2]", Level.INFO);
        assertLogContainsMessage(openCartLogAsserter, "Successfully replicated orders from OpenCart using TypeScript", Level.INFO);

        assertDatabaseETLCompletion();
    }

    private void assertLogContainsMessage(LogsAsserter logAsserter, String message, Level level) {
        await().atMost(45, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> logAsserter.containsMessage(message, level));
    }

    private void assertDatabaseETLCompletion() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();

        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema("DefaultDB");

            Table ordersTable = new Table(dataSource, "\"ORDERS\"");

            Assertions.assertThat(ordersTable)
                      .hasNumberOfRows(2);

            Assertions.assertThat(ordersTable)
                      .row(0)
                      .column("ID")
                      .value()
                      .isEqualTo(1)
                      .column("TOTAL")
                      .value()
                      .isEqualTo(92)

                      .row(1)
                      .column("ID")
                      .value()
                      .isEqualTo(2)
                      .column("TOTAL")
                      .value()
                      .isEqualTo(230.46);

        } catch (SQLException e) {
            LOGGER.error("Error while querying the ORDERS table: ", e);
        }
    }


}
