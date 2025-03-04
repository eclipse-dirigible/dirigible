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

import com.codeborne.selenide.WebDriverRunner;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.mail.EmailAsserter;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BPMLeaveRequestIT extends UserInterfaceIntegrationTest {

    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_PASSWORD = "randomPassword";
    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String EMPLOYEE_MANAGER_PASSWORD = "anotherPassword";

    private static final String MAIL_USER = "user";
    private static final String MAIL_PASSWORD = "password";
    private static final int MAIL_PORT = 56565;

    static {
        configureEmail();
    }

    @Autowired
    private BPMLeaveRequestTestProject testProject;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    private GreenMail greenMail;

    private static void configureEmail() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(MAIL_PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @BeforeEach
    void setUp() throws MessagingException {
        startGreenMailServer();

        testProject.copyToWorkspace();
        testProject.generateForms();
        testProject.publish();

        createSecurityUsers();

        submitLeaveRequest();
        assertNotificationEmailReceived();

        browser.clearCookies();
        claimRequest();
    }

    private void startGreenMailServer() {
        ServerSetup serverSetup = new ServerSetup(MAIL_PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);
        greenMail.start();
        greenMail.setUser(MAIL_USER, MAIL_PASSWORD);
    }

    private void submitLeaveRequest() {
        browser.clearCookies();
        IDE employeeIde = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        employeeIde.openPath("/services/web/" + testProject.getProjectResourcesFolder()
                + "/gen/submit-leave-request/forms/submit-leave-request/index.html");
        fillLeaveRequestForm();

        acceptAlert();
    }

    private void fillLeaveRequestForm() {
        browser.enterTextInElementById("fromId", "02/02/2002");
        browser.enterTextInElementById("toId", "03/03/2002");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Submit");
    }

    private void acceptAlert() {
        WebDriverWait wait = new WebDriverWait(WebDriverRunner.getWebDriver(), Duration.ofMillis(1000));
        wait.until(ExpectedConditions.alertIsPresent());

        Alert alert = WebDriverRunner.getWebDriver()
                                     .switchTo()
                                     .alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("Leave request has been created.");
        alert.accept();
    }

    private void createSecurityUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, "employee");
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD, "employee-manager");
    }

    private void claimRequest() {
        IDE managerIde = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        managerIde.openPath("/services/web/inbox/");

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");

        Set<String> windowHandles = WebDriverRunner.getWebDriver()
                                                   .getWindowHandles();

        String newTabHandle = windowHandles.toArray(new String[0])[windowHandles.size() - 1];
        WebDriverRunner.getWebDriver()
                       .switchTo()
                       .window(newTabHandle);
    }

    private void assertNotificationEmailReceived() throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 1);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[0];

        EmailAsserter.assertEmailReceived(sentEmail, "New leave request", "<h4>A new leave request for [" + EMPLOYEE_USERNAME
                + "] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.",
                "leave-request-app@example.com", "managers-dl@example.com");
    }

    @Test
    void testApproveLeaveRequest() throws MessagingException {
        processRequest(true);

        assertApprovalEmailReceived();
    }

    private void processRequest(boolean approve) {
        String buttonLabel = approve ? "Approve" : "Decline";
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, buttonLabel);

        acceptAlert();

        EmailAsserter.assertReceivedEmailsCount(greenMail, 2);

    }

    private void assertApprovalEmailReceived() throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 2);

        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAsserter.assertEmailReceived(sentEmail, "Your leave request has been approved",
                "has been approved by [" + EMPLOYEE_MANAGER_USERNAME + "]</h4>", "leave-request-app@example.com", EMPLOYEE_USERNAME);
    }

    @Test
    void testDeclineLeaveRequest() throws MessagingException {
        processRequest(false);

        assertDeclinedEmalReceived();
    }

    private void assertDeclinedEmalReceived() throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 2);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAsserter.assertEmailReceived(sentEmail, "Your leave request has been declined",
                "has been declined by [" + EMPLOYEE_MANAGER_USERNAME + "]</h4>", "leave-request-app@example.com", EMPLOYEE_USERNAME);
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }
}
