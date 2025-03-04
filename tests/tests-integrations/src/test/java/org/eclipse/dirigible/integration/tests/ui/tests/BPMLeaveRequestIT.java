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
import org.eclipse.dirigible.integration.tests.ui.tests.projects.LeaveRequestITTestProject;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

class BPMLeaveRequestIT extends UserInterfaceIntegrationTest {

    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_PASSWORD = "john.doe.employee@example.com";
    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String EMPLOYEE_MANAGER_PASSWORD = "emily.stone.mngr@example.com";

    private static final String MAIL_USER = "user";
    private static final String MAIL_PASSWORD = "password";
    private static final int MAIL_PORT = 56565;

    static {
        DirigibleConfig.MAIL_USERNAME.setStringValue(MAIL_USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(MAIL_PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(MAIL_PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @Autowired
    private LeaveRequestITTestProject leaveRequestTestProject;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    private GreenMail greenMail;

    @BeforeEach
    public void setUp() throws MessagingException {
        startGreenMailServer();
        leaveRequestTestProject.generateFromFormFiles();
        leaveRequestTestProject.publish(true);
        browser.clearCookies();
        createSecurityUsers();

        submitLeaveRequest();
        assertNotificationEmailSent();

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
        ide = createIdeForUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
        ide.openPath("/services/web/LeaveRequestApprovalProcessIT/gen/submit-leave-request/forms/submit-leave-request/index.html");
        fillFormAndSubmitIt();
    }

    private IDE createIdeForUser(String username, String password) {
        return ideFactory.create(username, password);
    }

    private void fillFormAndSubmitIt() {
        browser.enterTextInElementById("fromId", "02/02/2002");
        browser.enterTextInElementById("toId", "03/03/2002");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Submit");
        handleAlertAccept();
    }

    private void handleAlertAccept() {
        WebDriverWait wait = new WebDriverWait(WebDriverRunner.getWebDriver(), Duration.ofMillis(1000));
        wait.until(ExpectedConditions.alertIsPresent());

        Alert alert = WebDriverRunner.getWebDriver()
                                     .switchTo()
                                     .alert();
        alert.accept();
    }

    private void createSecurityUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, "employee");
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD, "employee-manager");
    }

    private void claimRequest() {
        IDE ide2 = createIdeForUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD);

        ide2.openPath("/services/web/inbox/");

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

    private void assertNotificationEmailSent() throws MessagingException {
        assertReceivedEmailsCount(1);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[0];

        EmailAsserter.assertEmailReceived(sentEmail, "New leave request",
                "<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.",
                "leave-request-app@example.com", "managers-dl@example.com");
    }

    private void assertReceivedEmailsCount(int expectedCount) {
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> greenMail.getReceivedMessages().length >= expectedCount);
    }

    @Test
    public void testCreateBPMProcessAndApproveIt() throws MessagingException {
        processRequest(true);

        testApprovalEmail();
    }

    private void processRequest(boolean approve) {
        String option = approve ? "Approve" : "Decline";
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, option);

        handleAlertAccept();

        assertReceivedEmailsCount(2);

    }

    private void testApprovalEmail() throws MessagingException {
        assertReceivedEmailsCount(2);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAsserter.assertEmailReceived(sentEmail, "Your leave request has been approved",
                "has been approved by [emily.stone.mngr@example.com]</h4>", "leave-request-app@example.com",
                "john.doe.employee@example.com");
    }

    @Test
    public void testCreateBPMProcessAndDeclineIt() throws MessagingException {
        processRequest(false);

        testDeclineEmail();
    }

    private void testDeclineEmail() throws MessagingException {
        assertReceivedEmailsCount(2);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAsserter.assertEmailReceived(sentEmail, "Your leave request has been declined",
                "has been declined by [emily.stone.mngr@example.com]</h4>", "leave-request-app@example.com",
                "john.doe.employee@example.com");
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
    }
}
