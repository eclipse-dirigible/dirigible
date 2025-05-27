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

import io.restassured.http.ContentType;
import org.eclipse.dirigible.tests.base.BaseMultitenantTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Lazy
@Component
class BpmnMultitenancyTestProject extends BaseMultitenantTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnMultitenancyTestProject.class);

    private final RestAssuredExecutor restAssuredExecutor;
    private final IDEFactory ideFactory;
    private final SecurityUtil securityUtil;

    BpmnMultitenancyTestProject(EdmView edmView, RestAssuredExecutor restAssuredExecutor, IDEFactory ideFactory, ProjectUtil projectUtil,
            SecurityUtil securityUtil) {
        super("BpmnMultitenancyIT", ideFactory.create(), projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
        this.ideFactory = ideFactory;
        this.securityUtil = securityUtil;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateForms("approve-employee-registration.form");
        publish();

        getIde().close();
    }

    @Override
    public void verify(DirigibleTestTenant tenant) {
        LOGGER.info("Verifying test project [{}] for tenant [{}]...", getProjectResourcesFolder(), tenant);

        String adminUser = tenant.getUsername();
        String adminPass = tenant.getPassword();
        securityUtil.assignAllSystemRolesToUser(adminUser, tenant.getId());

        String employeeManagerUsername = tenant.getId() + "-employeeManagerUsername";
        String employeeManagerPass = tenant.getId() + "-employeeManagerPass";
        securityUtil.createUser(tenant.getId(), employeeManagerUsername, employeeManagerPass, "employee-manager");

        String tenantHost = tenant.getHost();
        assertZeroBpmProcessInstances(tenantHost, adminUser, adminPass);

        String employeeName = tenant.getName() + "|name";
        triggerEmployeeRegistrationProcessExecution(tenant, employeeName);

        assertProcessInstancesCount(1, tenantHost, adminUser, adminPass);

        assertAdminUserDoesntHaveAccessToInboxTasks(tenantHost, adminUser, adminPass);
        assertEmployeeManagerHasAssignedTask(tenantHost, employeeManagerUsername, employeeManagerPass);

        approveEmployeeRegistration(tenantHost, employeeManagerUsername, employeeManagerPass);

        assertEmployeeIsRegistered(employeeName, tenantHost, employeeManagerUsername, employeeManagerPass);

        assertProcessInstancesCount(0, tenantHost, adminUser, adminPass);
        assertHistoricProcessInstancesCount(1, tenantHost, adminUser, adminPass);

        LOGGER.info("Test project [{}] for tenant [{}] has been verified successfully!", getProjectResourcesFolder(), tenant);
    }

    private void assertEmployeeIsRegistered(String employeeName, String tenantHost, String employeeManagerUsername,
            String employeeManagerPass) {
        restAssuredExecutor.execute(() -> given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                                 .when()
                                                 .get("/odata/v2/Employees")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("d.results", hasSize(1))
                                                 .body("d.results[0].Id", equalTo(1))
                                                 .body("d.results[0].Name", equalTo(employeeName)),
                tenantHost, employeeManagerUsername, employeeManagerPass);
    }

    private void approveEmployeeRegistration(String tenantHost, String employeeManagerUsername, String employeeManagerPass) {
        IDE ide = ideFactory.create(tenantHost, employeeManagerUsername, employeeManagerPass);
        ide.openInbox();

        Browser browser = ide.getBrowser();
        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");
        browser.switchToLatestTab();

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Approve");

        browser.assertAlertWithMessage("Request Approved");

        browser.close();
    }

    private void assertEmployeeManagerHasAssignedTask(String host, String employeeManagerUsername, String employeeManagerPass) {
        assertGroupInboxTasksCount(1, host, employeeManagerUsername, employeeManagerPass);
        assertUserAssignedInboxTasksCount(0, host, employeeManagerUsername, employeeManagerPass);
    }

    private void assertUserAssignedInboxTasksCount(int expectedCount, String host, String user, String password) {
        restAssuredExecutor.execute(() -> assertUserAssignedInboxTasksCount(expectedCount), host, user, password);
    }

    private void assertGroupInboxTasksCount(int expectedCount, String host, String username, String password) {
        restAssuredExecutor.execute(() -> assertGroupInboxTasksCount(expectedCount), host, username, password);
    }

    private void assertAdminUserDoesntHaveAccessToInboxTasks(String host, String adminUsername, String adminPass) {
        assertGroupInboxTasksCount(0, host, adminUsername, adminPass);
        assertUserAssignedInboxTasksCount(0, host, adminUsername, adminPass);
    }

    private void assertGroupInboxTasksCount(int expectedCount) {
        given().when()
               .get("/services/inbox/tasks?type=groups")
               .then()
               .statusCode(200)
               .body("", hasSize(expectedCount));
    }

    private static void assertUserAssignedInboxTasksCount(int expectedCount) {
        given().when()
               .get("/services/inbox/tasks?type=assignee")
               .then()
               .statusCode(200)
               .body("", hasSize(expectedCount));
    }

    private void triggerEmployeeRegistrationProcessExecution(DirigibleTestTenant tenant, String employeeName) {
        restAssuredExecutor.execute(tenant, () -> triggerEmployeeRegistrationProcess(employeeName));
    }

    private static void triggerEmployeeRegistrationProcess(String name) {
        given().contentType(ContentType.JSON)
               .body("{\"name\":\"" + name + "\"}")
               .when()
               .post("/services/ts/BpmnMultitenancyIT/ProcessService.ts/processes")
               .then()
               .statusCode(202);
    }

    private void assertZeroBpmProcessInstances(String host, String userName, String password) {
        restAssuredExecutor.execute(() -> {
            assertProcessInstancesCount(0, host, userName, password);
            assertHistoricProcessInstancesCount(0, host, userName, password);
        }, host, userName, password);
    }

    private void assertHistoricProcessInstancesCount(int expectedCount, String host, String userName, String password) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/historic-instances")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(expectedCount)),
                host, userName, password);
    }

    private void assertProcessInstancesCount(int expectedCount, String host, String userName, String password) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instances")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(expectedCount)),
                host, userName, password);
    }

}
