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
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * A cron-triggered route reaches its JavaScript component and carries the body that component set
 * through to the final log step.
 */
public class CamelDirigibleJavaScriptComponentCronRouteIT extends IntegrationTest {

    private static final String PROJECT = "CamelDirigibleJavaScriptComponentCronRouteIT";

    private LogsAsserter logsAsserter;

    @Autowired
    private ProjectDeployer projectDeployer;

    /**
     * Attaches the log asserter here and not in a field initializer: Spring re-initializes logback
     * while it starts the application context, which is after the test instance is constructed and
     * before this callback - an appender attached any earlier is dropped by that reset.
     */
    @BeforeEach
    void attachLogAsserter() {
        logsAsserter = new LogsAsserter("CustomComponentLogger", Level.INFO);
    }

    @Test
    void theRouteCompletesWithTheBodyItsComponentSet() {
        projectDeployer.deploy(PROJECT);

        // this log message is expected to be logged by the final camel log step
        await().atMost(20, TimeUnit.SECONDS)
               .until(() -> logsAsserter.containsMessage("Completed execution. Body: [MY TEST BODY]", Level.INFO));
    }
}
