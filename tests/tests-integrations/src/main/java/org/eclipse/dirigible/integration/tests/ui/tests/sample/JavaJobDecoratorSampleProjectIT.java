/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;

import ch.qos.logback.classic.Level;

public class JavaJobDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-job-decorator.git";
    }

    @Override
    protected void verifyProject() {
        // Self-describing interface style — CleanupJob implements JobHandler (schedule from cron()).
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("CleanupJob executed!", Level.INFO));

        // Method-level annotation style — Maintenance's @Scheduled method.
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Maintenance.purgeTempFiles executed", Level.INFO));
    }

}
