/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Lazy
@Component
class CamelTypescriptTestProject extends BaseTestProject {
    private LogsAsserter camelLogAsserter;
    private LogsAsserter consoleLogAsserter;

    public CamelTypescriptTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super("CamelExtractTransformLoadIT_testTypeScriptScenario", ide, projectUtil, edmView);
    }

    @Override
    public void verify() {
        assertLogContainsMessage(camelLogAsserter, "Replicating orders from OpenCart using TypeScript", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "About to upsert Open cart order [1] using exchange rate", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "Upserted Open cart order [1]", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "About to upsert Open cart order [2] using exchange rate", Level.INFO);
        assertLogContainsMessage(consoleLogAsserter, "Upserted Open cart order [2]", Level.INFO);
        assertLogContainsMessage(camelLogAsserter, "Successfully replicated orders from OpenCart using TypeScript", Level.INFO);
    }

    private void assertLogContainsMessage(LogsAsserter logAsserter, String message, Level level) {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> logAsserter.containsMessage(message, level));
    }

    public void setLogsAsserter(LogsAsserter camelLogAsserter, LogsAsserter consoleLogAsserter) {
        this.camelLogAsserter = camelLogAsserter;
        this.consoleLogAsserter = consoleLogAsserter;
    }
}
