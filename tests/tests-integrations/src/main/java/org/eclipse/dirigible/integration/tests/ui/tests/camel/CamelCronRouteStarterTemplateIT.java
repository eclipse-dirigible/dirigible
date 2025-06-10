/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.WelcomeView;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class CamelCronRouteStarterTemplateIT extends UserInterfaceIntegrationTest {

    private static final String TEMPLATE_TITLE = "Cron Route Project Starter";
    private static final String TEST_PROJECT = CamelCronRouteStarterTemplateIT.class.getSimpleName();

    private LogsAsserter camelRouteLogAsserter;

    @BeforeEach
    void setUp() {
        this.camelRouteLogAsserter = new LogsAsserter("CronRouteLogger", Level.INFO);
    }

    @Test
    void testCreateProjectFromTemplate() {
        Workbench workbench = ide.openWorkbench();

        WelcomeView welcomeView = workbench.openWelcomeView();
        welcomeView.searchForTemplate(TEMPLATE_TITLE);
        welcomeView.selectTemplate(TEMPLATE_TITLE);

        welcomeView.typeProjectName(TEST_PROJECT);
        welcomeView.typeFileName(TEST_PROJECT);
        welcomeView.confirmTemplate();

        workbench.publishAll(true);
        SynchronizationUtil.waitForSynchronizationExecution();

        await().atMost(15, TimeUnit.SECONDS)
               .until(() -> camelRouteLogAsserter.containsMessage("Executing cron route with body []...", Level.INFO)
                       && camelRouteLogAsserter.containsMessage("Execution completed! Body: Set by handler.ts", Level.INFO));
    }
}
