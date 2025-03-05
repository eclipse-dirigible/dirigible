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
import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

class CamelExtractTransformLoadIT extends UserInterfaceIntegrationTest {

    @Autowired
    private CamelJDBCTestProject jdbcTestProject;

    @Autowired
    private CamelTypescriptTestProject typescriptTestProject;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    private LogsAsserter consoleLogAsserter;
    private LogsAsserter camelLogAsserter;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
        this.camelLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);
    }

    @Test
    void testJDBCScenario() {
        jdbcTestProject.setLogsAsserter(camelLogAsserter);
        jdbcTestProject.copyToWorkspace();
        jdbcTestProject.publish();

        jdbcTestProject.verify();

        assertDatabaseETLCompletion();
    }

    @Test
    void testTypeScriptScenario() {
        typescriptTestProject.setLogsAsserter(camelLogAsserter, consoleLogAsserter);
        typescriptTestProject.copyToWorkspace();
        typescriptTestProject.publish();

        typescriptTestProject.verify();

        assertDatabaseETLCompletion();
    }



    private void assertDatabaseETLCompletion() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table ordersTable = connection.table("\"ORDERS\"")
                                      .build();

        Assertions.assertThat(ordersTable)
                  .hasNumberOfRows(2)
                  .row(0)
                  .value("ID")
                  .isEqualTo(1)
                  .value("TOTAL")
                  .isEqualTo(92)
                  .row(1)
                  .value("ID")
                  .isEqualTo(2)
                  .value("TOTAL")
                  .isEqualTo(230.46);
    }



}
