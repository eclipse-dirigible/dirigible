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
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.FormView;
import org.eclipse.dirigible.tests.framework.ide.WelcomeView;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class BPMStarterTemplateIT extends UserInterfaceIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BPMStarterTemplateIT.class);

    private static final String TEMPLATE_TITLE = "BPM Starter Project";
    private static final String TEST_PROJECT = "bpm-test-project";
    private static final String TEST_PROCESS = "bpm-test-process";
    private static final String PROCESS_IDENTIFIER_ID = "param_processId";
    private static final String PROCESS_ID = "process-identifier";
    private static final String TRIGGER_PROCESS_FORM_FILENAME = "trigger-new-process.form";
    private static final String TRIGGER_PROCESS_FORM_PATH =
            "/services/web/" + TEST_PROJECT + "/gen/trigger-new-process/forms/trigger-new-process/index.html";
    private static final String PARAM_1_ID = "param1Id";
    private static final String PARAM_2_ID = "param2Id";
    private static final String PARAM_1_VALUE = "string-param-value";
    private static final String PARAM_2_VALUE = "777";
    public static final String EXPECTED_TASK_LOGGED_MESSAGE =
            "Hello World! Process variables: {param1=" + PARAM_1_VALUE + ", param2=" + PARAM_2_VALUE + ".0}";
    private static final String TRIGGER_BUTTON_TEXT = "Trigger";

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Test
    void testCreateProjectFromTemplate() {
        LOGGER.debug("Creating test project [{}] from template [{}]...", TEST_PROJECT, TEMPLATE_TITLE);
        Workbench workbench = ide.openWorkbench();

        WelcomeView welcomeView = workbench.openWelcomeView();
        welcomeView.searchForTemplate(TEMPLATE_TITLE);
        welcomeView.selectTemplate(TEMPLATE_TITLE);

        welcomeView.typeProjectName(TEST_PROJECT);
        welcomeView.typeFileName(TEST_PROCESS);
        welcomeView.typeTemplateParamById(PROCESS_IDENTIFIER_ID, PROCESS_ID);
        welcomeView.confirmTemplate();
        LOGGER.debug("Created test project [{}] from template [{}]...", TEST_PROJECT, TEMPLATE_TITLE);

        workbench.expandProject(TEST_PROJECT);
        workbench.openFile(TRIGGER_PROCESS_FORM_FILENAME);

        FormView formView = workbench.getFormView();
        LOGGER.debug("Regenerating form [{}]...", TRIGGER_PROCESS_FORM_FILENAME);

        formView.regenerateForm();
        ide.assertStatusBarMessage("Generated from model '" + TRIGGER_PROCESS_FORM_FILENAME + "'");
        LOGGER.debug("Form [{}] has been regenerated.", TRIGGER_PROCESS_FORM_FILENAME);

        LOGGER.debug("Publishing all projects...", TRIGGER_PROCESS_FORM_FILENAME);
        workbench.publishAll(true);
        LOGGER.debug("All projects have been published and the synchronization is completed.", TRIGGER_PROCESS_FORM_FILENAME);

        browser.openPath(TRIGGER_PROCESS_FORM_PATH);
        browser.enterTextInElementById(PARAM_1_ID, PARAM_1_VALUE);
        browser.enterTextInElementById(PARAM_2_ID, PARAM_2_VALUE);
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, TRIGGER_BUTTON_TEXT);

        LOGGER.debug("Asserting that a new process instance has been triggered...");
        browser.assertAlertWithMessage("A new process instance has been triggered");

        await().atMost(30, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage(EXPECTED_TASK_LOGGED_MESSAGE, Level.INFO));
    }
}
