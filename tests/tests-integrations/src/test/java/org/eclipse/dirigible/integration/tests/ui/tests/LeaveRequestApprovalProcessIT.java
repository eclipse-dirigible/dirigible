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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.integration.tests.ui.LeaveRequestTestProject;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LeaveRequestApprovalProcessIT extends UserInterfaceIntegrationTest {
    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String SUBMIT_FORM_URL =
            "/services/web/LeaveRequestApprovalProcessIT/gen/submit-leave-request/forms/submit-leave-request/index.html";
    private static final String INBOX_URL = "/services/web/inbox/";

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final int PORT = 56565;
    static {
        DirigibleConfig.MAIL_USERNAME.setStringValue(USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @Autowired
    private LeaveRequestTestProject leaveRequestTestProject;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    private GreenMail greenMail;

    @BeforeEach
    public void setUp() throws MessagingException {
        ServerSetup serverSetup = new ServerSetup(PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);

        greenMail.start();

        greenMail.setUser(USER, PASSWORD);

        leaveRequestTestProject.publish();

        browser.clearCookies();

        createSecurityUsers();

        ide = createIdeForUser(EMPLOYEE_USERNAME);

        ide.openPath(SUBMIT_FORM_URL);

        fillFormAndSubmitIt();

        assertNotificationEmailSent();

        browser.clearCookies();

        IDE ide2 = createIdeForUser(EMPLOYEE_MANAGER_USERNAME);

        claimRequest(ide2);
    }

    @Test
    public void testCreateBPMProcessAndApproveIt() throws MessagingException {
        processRequest(true);

        testApprovalEmail();
    }

    @Test
    public void testCreateBPMProcessAndDeclineIt() throws MessagingException {
        processRequest(false);

        testDeclineEmail();
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
    }

    private void createSecurityUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME, "employee");
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME, "employee-manager");
    }

    private IDE createIdeForUser(String usernameAndPassword) {
        return ideFactory.create(usernameAndPassword, usernameAndPassword);
    }

    private void fillFormAndSubmitIt() {
        browser.enterTextInElementById("fromId", "02/02/2002");
        browser.enterTextInElementById("toId", "03/03/2002");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Submit");
    }


    private void claimRequest(IDE ide2) {
        ide2.openPath(INBOX_URL);

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");

        String firstTdText = browser.getFirstTdTextInRowContaining("Process request");
        browser.openPath(
                "/services/web/LeaveRequestApprovalProcessIT/gen/process-leave-request/forms/process-leave-request/index.html?taskId=" + firstTdText);
        // browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");
        // this doesnt work because it opens in a new tab and it should be just redirected
    }

    private void processRequest(boolean approve) {
        // browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Decline");
        // it doesnt work because it has something that prevents it from clicking it
        String option = approve ? "Approve" : "Decline";
        SelenideElement approveButton = browser.findElementInAllFrames(Selectors.byText(option), Condition.visible);
        Selenide.executeJavaScript("arguments[0].click();", approveButton);

        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> greenMail.getReceivedMessages().length >= 2);

    }

    private void testDeclineEmail() throws MessagingException {
        awaitEmailReceived(2);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAssertions.assertEmailReceived(sentEmail,
                "Your leave request has been declined",
                "has been declined by [emily.stone.mngr@example.com]</h4>");
    }

    private void testApprovalEmail() throws MessagingException {
        awaitEmailReceived(2);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[1];
        EmailAssertions.assertEmailReceived(sentEmail,
                "Your leave request has been approved",
                "has been approved by [emily.stone.mngr@example.com]</h4>");
    }

    private void assertNotificationEmailSent() throws MessagingException {
        awaitEmailReceived(1);
        MimeMessage sentEmail = greenMail.getReceivedMessages()[0];

        assertThat(sentEmail.getSubject()).isEqualTo("New leave request");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("managers-dl@example.com");
        assertThat(GreenMailUtil.getBody(sentEmail).trim()).contains(
                "<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.");
    }

    private void awaitEmailReceived(int expectedCount) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length >= expectedCount);
    }
}
