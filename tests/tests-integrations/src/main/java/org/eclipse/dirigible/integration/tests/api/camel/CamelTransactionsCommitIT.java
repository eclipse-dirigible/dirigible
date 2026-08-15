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
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * A route whose JavaScript step saves entities and completes leaves those entities committed.
 */
public class CamelTransactionsCommitIT extends IntegrationTest {

    private static final String PROJECT = "CamelTransactionsCommitIT";

    private static final String MODEL = "edm.model";

    private LogsAsserter logsAsserter;

    @Autowired
    private ProjectDeployer projectDeployer;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    /**
     * Attaches the log asserter here and not in a field initializer: Spring re-initializes logback
     * while it starts the application context, which is after the test instance is constructed and
     * before this callback - an appender attached any earlier is dropped by that reset.
     */
    @BeforeEach
    void attachLogAsserter() {
        logsAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Test
    void theSavedEntitiesAreCommitted() throws SQLException {
        projectDeployer.deployGeneratedFromModel(PROJECT, MODEL);

        await().atMost(10, TimeUnit.SECONDS)
               .pollDelay(1, TimeUnit.SECONDS)
               .until(() -> logsAsserter.containsMessage("camel-handler.ts: test entities are saved", Level.INFO));

        assertTestTableSize();
    }

    private void assertTestTableSize() throws SQLException {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            int count = dialect.count(connection, "BOOK");
            assertThat(count).isGreaterThanOrEqualTo(3);
        }
    }
}
