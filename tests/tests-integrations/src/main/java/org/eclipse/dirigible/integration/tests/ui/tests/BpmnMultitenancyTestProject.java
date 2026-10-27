/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Reproduction for #8078: {@code POST /bpm-processes/instance/{id}/variables} and the SKIP action
     * do not validate that the target instance belongs to the caller's tenant, so one tenant's operator
     * can write into another tenant's running process. The victim tenant starts a process that parks at
     * its approval user task; the attacker tenant - a different tenant whose operator has no business
     * touching the victim's process - then writes a variable and plants the SKIP marker on the victim's
     * instance id. The assertions encode the secure expectation (reject, nothing written), so this test
     * is red on the unfixed code and becomes the regression guard once the validation is added.
     *
     * @param tenants the provisioned tenants; at least two are required
     */
    void demonstrateCrossTenantVariableWrite(List<DirigibleTestTenant> tenants) {
        if (tenants.size() < 2) {
            throw new IllegalStateException("At least two tenants are required to demonstrate cross-tenant isolation");
        }
        configure();

        DirigibleTestTenant victim = tenants.get(1);
        DirigibleTestTenant attacker = tenants.get(2);

        securityUtil.assignAllSystemRolesToUser(victim.getUsername(), victim.getId());
        securityUtil.assignAllSystemRolesToUser(attacker.getUsername(), attacker.getId());

        restAssuredExecutor.execute(victim, () -> triggerEmployeeRegistrationProcess(victim.getName() + "|victim"));

        String victimInstanceId = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                                     .get("/services/bpm/bpm-processes/instances")
                                                                                     .then()
                                                                                     .statusCode(200)
                                                                                     .body("", hasSize(1))
                                                                                     .extract()
                                                                                     .path("[0].id"),
                victim.getHost(), victim.getUsername(), victim.getPassword());
        LOGGER.info("[#8078] victim tenant [{}] has running instance id [{}]", victim.getId(), victimInstanceId);

        int variableWriteStatus = restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                                     .body("{\"name\":\"pwnedByAttacker\",\"value\":\"attacker-controlled\"}")
                                                                                     .when()
                                                                                     .post("/services/bpm/bpm-processes/instance/"
                                                                                             + victimInstanceId + "/variables")
                                                                                     .then()
                                                                                     .extract()
                                                                                     .statusCode(),
                attacker.getHost(), attacker.getUsername(), attacker.getPassword());
        LOGGER.info("[#8078] attacker tenant [{}] variable write into victim instance returned HTTP [{}]", attacker.getId(),
                variableWriteStatus);

        int skipStatus = restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                            .body("{\"action\":\"SKIP\"}")
                                                                            .when()
                                                                            .post("/services/bpm/bpm-processes/instance/"
                                                                                    + victimInstanceId)
                                                                            .then()
                                                                            .extract()
                                                                            .statusCode(),
                attacker.getHost(), attacker.getUsername(), attacker.getPassword());
        LOGGER.info("[#8078] attacker tenant [{}] SKIP on victim instance returned HTTP [{}]", attacker.getId(), skipStatus);

        List<String> victimVariableNames = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                                              .get("/services/bpm/bpm-processes/instance/"
                                                                                                      + victimInstanceId + "/variables")
                                                                                              .then()
                                                                                              .statusCode(200)
                                                                                              .extract()
                                                                                              .path("name"),
                victim.getHost(), victim.getUsername(), victim.getPassword());
        LOGGER.info("[#8078] victim instance variable names after the attack: {}", victimVariableNames);

        assertTrue(variableWriteStatus == 403 || variableWriteStatus == 404,
                "A different tenant must not be allowed to write a variable into the victim's instance, but the endpoint answered HTTP "
                        + variableWriteStatus);
        assertFalse(victimVariableNames.contains("pwnedByAttacker"),
                "Attacker-controlled variable 'pwnedByAttacker' must not be present on the victim's instance");
        // DIRIGIBLE_BPM_INTERNAL_SKIP_STEP - the internal skip marker; a foreign tenant must not be able to
        // plant it.
        assertFalse(victimVariableNames.contains("DIRIGIBLE_BPM_INTERNAL_SKIP_STEP"),
                "The internal SKIP marker must not be plantable on the victim's instance by another tenant");
    }

    private void assertEmployeeIsRegistered(String employeeName, String tenantHost, String employeeManagerUsername,
            String employeeManagerPass) {
        restAssuredExecutor.execute(() -> given().header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                                 .when()
                                                 .get("/services/ts/BpmnMultitenancyIT/ProcessService.ts/employees")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1))
                                                 .body("[0].ID", equalTo(1))
                                                 .body("[0].NAME", equalTo(employeeName)),
                tenantHost, employeeManagerUsername, employeeManagerPass);
    }

    private void approveEmployeeRegistration(String tenantHost, String employeeManagerUsername, String employeeManagerPass) {
        IDE ide = ideFactory.create(tenantHost, employeeManagerUsername, employeeManagerPass);
        ide.openInbox();

        Browser browser = ide.getBrowser();
        // The redesigned inbox is an Outlook-style master-detail list: tasks are bk-list links (not
        // table rows), and claiming embeds the task's form in the detail pane (no "Open Form" tab).
        browser.clickOnElementContainingText(HtmlElementType.ANCHOR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");

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
