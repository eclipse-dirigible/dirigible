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
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BPMProcessIT extends UserInterfaceIntegrationTest {
    private static final String EMPLOYEE_ROLE = "employee";
    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_MANAGER_ROLE = "employee-manager";
    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String SUBMIT_FORM_URL =
            "/services/web/leave-request/gen/submit-leave-request/forms/submit-leave-request/index.html";
    private static final String INBOX_URL = "/services/web/inbox/";
    private static final String SUBMIT_BUTTON_TEXT = "Submit";

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
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private IDEFactory ideFactory;

    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        ServerSetup serverSetup = new ServerSetup(PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);

        greenMail.start();

        greenMail.setUser(USER, PASSWORD);

        leaveRequestTestProject.publish();

        browser.clearCookies();

        // wait some time synchronizers to complete their execution
        SleepUtil.sleepSeconds(10);
    }

    @Test
    void testCreateBPMProcessAndApproveIt() throws MessagingException {
        // Step 1: Create users
        createSecurityUsers();

        // Step 2: Log in as employee
        ide = createIdeFromUser(EMPLOYEE_USERNAME);

        // Open the submit form
        ide.openPath(SUBMIT_FORM_URL);

        // Fill the form and send it
        fillForm();

        // Waits for the email to be sent
        SleepUtil.sleepSeconds(5);

        // Test if the email has been sent
        testSendEmailForm();

        // Clears cookies but should check why it only works with this:
        // It works bc it isnt logged in otherwise as emily
        browser.clearCookies();

        // Step 3: Logs in as a manager and decline
        ide = createIdeFromUser(EMPLOYEE_MANAGER_USERNAME);

        processRequest();

        declineOrApproveRequest("Approve");

        SleepUtil.sleepSeconds(5);

        testApprovalEmail();
    }

    @Test
    void testCreateBPMProcessAndDeclineIt() throws MessagingException {
        // Step 1: Create users
        createSecurityUsers();

        // Step 2: Log in as employee
        ide = createIdeFromUser(EMPLOYEE_USERNAME);

        // Open the submit form
        ide.openPath(SUBMIT_FORM_URL);

        // Fill the form and send it
        fillForm();

        // Waits for the email to be sent
        SleepUtil.sleepSeconds(5);

        // Test if the email has been sent
        testSendEmailForm();

        // Clears cookies but should check why it only works with this
        browser.clearCookies();

        // Step 3: Logs in as a manager and decline
        ide = createIdeFromUser(EMPLOYEE_MANAGER_USERNAME);

        processRequest();

        declineOrApproveRequest("Decline");

        testDeclineEmail();
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
        browser.clearCookies();
    }

    public void createSecurityUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME, EMPLOYEE_ROLE);
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_ROLE);
    }

    public IDE createIdeFromUser(String UsernameAndPassword) {
        return ideFactory.create(UsernameAndPassword, UsernameAndPassword);
    }

    public void fillForm() {
        browser.enterTextInElementById("fromId", "02/02/2002");
        browser.enterTextInElementById("toId", "03/03/2002");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, SUBMIT_BUTTON_TEXT);
    }


    public void processRequest() {
        ide.openPath(INBOX_URL);

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");

        String firstTdText = browser.getFirstTdTextInRowContaining("Process request"); // this gets the id bc it is not always 17
        browser.openPath(
                "/services/web/leave-request/gen/process-leave-request/forms/process-leave-request/index.html?taskId=" + firstTdText);
        // browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");
    }

    public void declineOrApproveRequest(String option) {
        // browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Decline"); //check why this doesnt
        // work
        SelenideElement approveButton = browser.findElementInAllFrames(Selectors.byText(option), Condition.visible);
        Selenide.executeJavaScript("arguments[0].click();", approveButton);

        SleepUtil.sleepSeconds(5);

    }

    void testDeclineEmail() throws MessagingException {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(2);

        MimeMessage sentEmail = receivedMessages[1];
        assertThat(sentEmail.getSubject()).isEqualTo("Your leave request has been declined");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("john.doe.employee@example.com");
        String emailBody = GreenMailUtil.getBody(sentEmail)
                                        .trim();

        String extractedFromDate = emailBody.split("from \\[")[1].split("T")[0];
        String extractedToDate = emailBody.split("to \\[")[1].split("T")[0];

        assertThat(extractedFromDate).isEqualTo("2002-02-02");
        assertThat(extractedToDate).isEqualTo("2002-03-03");

        assertThat(GreenMailUtil.getBody(sentEmail)
                                .trim()).contains("has been declined by [emily.stone.mngr@example.com]</h4>");
    }

    void testApprovalEmail() throws MessagingException {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(2);

        MimeMessage sentEmail = receivedMessages[1];

        assertThat(sentEmail.getSubject()).isEqualTo("Your leave request has been approved");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("john.doe.employee@example.com");
        String emailBody = GreenMailUtil.getBody(sentEmail)
                                        .trim();

        String extractedFromDate = emailBody.split("from \\[")[1].split("T")[0];
        String extractedToDate = emailBody.split("to \\[")[1].split("T")[0];

        assertThat(extractedFromDate).isEqualTo("2002-02-02");
        assertThat(extractedToDate).isEqualTo("2002-03-03");

        assertThat(GreenMailUtil.getBody(sentEmail)
                                .trim()).contains("has been approved by [emily.stone.mngr@example.com]</h4>");
    }

    void testSendEmailForm() throws MessagingException {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage sentEmail = receivedMessages[0];

        assertThat(sentEmail.getSubject()).isEqualTo("New leave request");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("managers-dl@example.com");
        assertThat(GreenMailUtil.getBody(sentEmail)
                                .trim()).contains(
                                        "<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.");
    }
}
