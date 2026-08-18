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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * A route whose JavaScript step saves an entity and then fails leaves nothing behind - the save is
 * rolled back with the route.
 */
@Disabled("Disabled until transaction logic is implemented")
public class CamelTransactionsRollbackIT extends IntegrationTest {

    private static final String PROJECT = "CamelTransactionsRollbackIT";

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
    void theSavedEntityIsRolledBack() {
        projectDeployer.deployGeneratedFromModel(PROJECT, MODEL);

        await().atMost(10, TimeUnit.SECONDS)
               .pollDelay(1, TimeUnit.SECONDS)
               .until(() -> logsAsserter.containsMessage("camel-handler.ts: an entity is saved", Level.INFO));

        assertDaoSaveIsRollbacked();
    }

    private void assertDaoSaveIsRollbacked() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();

        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table ordersTable = connection.table("BOOK")
                                      .build();

        Assertions.assertThat(ordersTable)
                  .hasNumberOfRows(0);
    }
}
