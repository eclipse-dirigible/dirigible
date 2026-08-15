/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.camel;

import ch.qos.logback.classic.Level;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * What the two order-replication routes share: the loggers they report progress on, and the table
 * the replication is expected to have loaded. Each replication owns the {@code ORDERS} table it
 * asserts row by row, which is why the two run in instances of their own rather than as two methods
 * of one class.
 */
abstract class BaseExtractTransformLoadIT extends IntegrationTest {

    /** The logger the routes themselves log their progress on. */
    protected LogsAsserter camelLogAsserter;

    /** The logger the JavaScript the routes call logs on. */
    protected LogsAsserter consoleLogAsserter;

    /** Brings the replication's own fixture project live. */
    @Autowired
    protected ProjectDeployer projectDeployer;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    /**
     * Attaches the log asserters here and not in a field initializer: Spring re-initializes logback
     * while it starts the application context, which is after the test instance is constructed and
     * before this callback - an appender attached any earlier is dropped by that reset, and the
     * assertions then time out against messages the log plainly shows.
     */
    @BeforeEach
    final void attachLogAsserters() {
        camelLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);
        consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    protected void assertLogContainsMessage(LogsAsserter logAsserter, String message, Level level) {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> logAsserter.containsMessage(message, level));
    }

    protected void assertDatabaseETLCompletion() {
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
