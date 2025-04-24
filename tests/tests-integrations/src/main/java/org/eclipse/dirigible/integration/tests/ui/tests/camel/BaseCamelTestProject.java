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
import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.base.TestProject;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public abstract class BaseCamelTestProject extends BaseTestProject implements TestProject {

    protected final LogsAsserter camelLogAsserter;
    protected final LogsAsserter consoleLogAsserter;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    protected BaseCamelTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super(projectResourcesFolder, ide, projectUtil, edmView);
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
        this.camelLogAsserter = new LogsAsserter("OpenCartOrdersReplication", Level.INFO);
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
