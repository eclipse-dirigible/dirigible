/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.projects;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.framework.BrowserFactory;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Lazy
@Component
public class LeaveRequestITTestProject extends BaseTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveRequestITTestProject.class);

    private static final String PROJECT_ROOT_FOLDER = "BPMLeaveRequestIT";
    private static final String API_PATH = "/services/ts/" + PROJECT_ROOT_FOLDER + "/api/ProcessService.ts";
    private static final String UI_HOME_PATH = "/services/web/" + PROJECT_ROOT_FOLDER + "/gen/index.html";
    private static final String PROCESS_LEAVE_REQUEST_FORM_FILENAME = "process-leave-request.form";
    private static final String SUBMIT_LEAVE_REQUEST_FORM_FILENAME = "submit-leave-request.form";

    private final BrowserFactory browserFactory;
    private final IDE ide;
    private final RestAssuredExecutor restAssuredExecutor;
    private final IDEFactory ideFactory;
    private final ProjectUtil projectUtil;
    private final LogsAsserter testLogsAsserter;

    public LeaveRequestITTestProject(BrowserFactory browserFactory, IDE ide, RestAssuredExecutor restAssuredExecutor, IDEFactory ideFactory,
            ProjectUtil projectUtil) {
        super(PROJECT_ROOT_FOLDER, ideFactory.create(), projectUtil);
        this.browserFactory = browserFactory;
        this.ide = ide;
        this.restAssuredExecutor = restAssuredExecutor;
        this.ideFactory = ideFactory;
        this.projectUtil = projectUtil;
        this.testLogsAsserter = new LogsAsserter("leave-request-process.bpmn", Level.DEBUG);
    }

    public void generateFromFormFiles() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(PROJECT_ROOT_FOLDER);
        Workbench workbench = ide.openWorkbench();
        workbench.expandProject(PROJECT_ROOT_FOLDER);
        workbench.openFile(PROCESS_LEAVE_REQUEST_FORM_FILENAME);

        regenerateForm(workbench, PROCESS_LEAVE_REQUEST_FORM_FILENAME);
        regenerateForm(workbench, SUBMIT_LEAVE_REQUEST_FORM_FILENAME);
    }

    private void regenerateForm(Workbench workbench, String fileName) {
        workbench.openFile(fileName);
        FormView formView = workbench.getFormView();
        formView.regenerateForm();
        ide.assertStatusBarMessage("Generated from model '" + fileName + "'");
    }

    @Override
    public void verify() {
        // something
    }
}
