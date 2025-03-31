/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Lazy
@Component
class CamelTransactionsCommitTestProject extends BaseTestProject {

    private final DataSourcesManager dataSourcesManager;
    private final LogsAsserter logsAsserter;

    protected CamelTransactionsCommitTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, DataSourcesManager dataSourcesManager) {
        super("CamelTransactionsCommitIT", ide, projectUtil, edmView);
        this.dataSourcesManager = dataSourcesManager;
        this.logsAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    public void configure() {
        copyToWorkspace();
        generateEDM("edm.edm");
        publish();
    }

    @Override
    public void verify() throws Exception {
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
