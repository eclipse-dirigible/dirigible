/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui;

import ch.qos.logback.classic.Level;
import io.restassured.http.ContentType;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.awaitility.AwaitilityExecutor;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.BrowserFactory;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Lazy
@Component
public class LeaveRequestTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveRequestTestProject.class);

    private static final String PROJECT_ROOT_FOLDER = "leave-request";
    private static final String API_PATH = "/services/ts/leave-request/api/ProcessService.ts";
    private static final String UI_HOME_PATH = "/services/web/leave-request/gen/index.html";

    private final BrowserFactory browserFactory;
    private final IDE ide;
    private final RestAssuredExecutor restAssuredExecutor;
    private final IDEFactory ideFactory;
    private final ProjectUtil projectUtil;
    private final LogsAsserter testLogsAsserter;

    public LeaveRequestTestProject(BrowserFactory browserFactory, IDE ide, RestAssuredExecutor restAssuredExecutor, IDEFactory ideFactory,
            ProjectUtil projectUtil) {
        this.browserFactory = browserFactory;
        this.ide = ide;
        this.restAssuredExecutor = restAssuredExecutor;
        this.ideFactory = ideFactory;
        this.projectUtil = projectUtil;
        this.testLogsAsserter = new LogsAsserter("leave-request-process.bpmn", Level.DEBUG);
    }

    public void publish() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(PROJECT_ROOT_FOLDER);
        Workbench workbench = ide.openWorkbench();
        workbench.expandProject(PROJECT_ROOT_FOLDER);
<<<<<<< HEAD
        workbench.openFile(PROCESS_LEAVE_REQUEST_FORM_FILENAME);

        FormView formView = workbench.getFormView();
        formView.regenerateForm();
        ide.assertStatusBarMessage("Generated from model '" + PROCESS_LEAVE_REQUEST_FORM_FILENAME + "'");

        workbench.openFile(SUBMIT_LEAVE_REQUEST_FORM_FILENAME);
        formView.regenerateForm();
        ide.assertStatusBarMessage("Generated from model '" + SUBMIT_LEAVE_REQUEST_FORM_FILENAME + "'");

        workbench.clickPublishAll();
        workbench.publishAll(true);
=======
        workbench.publishAll();
    }

    public void verify(DirigibleTestTenant tenant) {
        LOGGER.info("Verifying leave-request project for tenant [{}]...", tenant);
        try {
            verifyHomePage(tenant);
            verifyProcessService(tenant);
            LOGGER.info("Leave-request project verified successfully!");
        } catch (RuntimeException | Error ex) {
            throw new AssertionError("Failed to verify leave-request project", ex);
        }
    }

    private void verifyHomePage(DirigibleTestTenant tenant) {
        Browser browser = browserFactory.createByHost(tenant.getHost());
        browser.openPath(UI_HOME_PATH);
        IDE ide = ideFactory.create(browser, tenant.getUsername(), tenant.getPassword());
        ide.login(!tenant.isDefaultTenant());
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER3, "Leave Request System");
    }

    private void verifyProcessService(DirigibleTestTenant tenant) {
        String jsonPayload = "{\"requester\": \"John Doe\", \"daysRequested\": 5}";
        given().contentType(ContentType.JSON)
               .body(jsonPayload)
               .when()
               .post(API_PATH)
               .then()
               .statusCode(201);
        given().when()
               .get(API_PATH)
               .then()
               .statusCode(200)
               .body("$.requester", equalTo("John Doe"))
               .body("$.daysRequested", equalTo(5));
>>>>>>> 705ecb8ba8 (feat: adding tests for hyperion)
    }
}
