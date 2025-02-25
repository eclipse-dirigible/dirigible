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
import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class ApacheCamelIT extends UserInterfaceIntegrationTest {

    @Autowired
    private TestProject testProject;
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
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_JDBC_START_MESSAGE, Level.INFO));
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage(EXPECTED_JDBC_SUCCESS_MESSAGE, Level.INFO));

        // CHECK DATABASE IF THE DATA IS REPLICATED

        // CHECK IF THE CONVERSION IS SUCCESSFUL

    }

    @Test
    public void testImplementETLUsingTypescript() {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage("Replicating orders from OpenCart using TypeScript...", Level.INFO));

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_1_START, Level.INFO));

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_1_SUCCESS, Level.INFO));

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_2_START, Level.INFO));

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_UPSERT_ORDER_2_SUCCESS, Level.INFO));

        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(5, TimeUnit.SECONDS)
               .until(() -> openCartLogAsserter.containsMessage("Successfully replicated orders from OpenCart using TypeScript",
                       Level.INFO));
        // CHECK DATABASE IF THE DATA IS REPLICATED

        // CHECK IF THE CONVERSION IS SUCCESSFUL

    }

    @AfterEach
    public void tearDown() {
        browser.clearCookies();
    }

}

