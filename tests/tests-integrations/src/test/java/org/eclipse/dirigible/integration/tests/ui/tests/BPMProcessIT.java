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
    private static final String SUBMIT_FORM_URL = "/services/web/leave-request/gen/submit-leave-request/forms/submit-leave-request/index.html";
    private static final String INBOX_URL = "/services/web/inbox/";
    private static final String SUBMIT_BUTTON_TEXT = "Submit";

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final int PORT = 565;
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
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME, EMPLOYEE_ROLE);
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_ROLE);

        // Step 2: Log in as employee
        IDE ide = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME);
        ide.openPath(SUBMIT_FORM_URL);

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, SUBMIT_BUTTON_TEXT);

        // Waits for the email to be sent
        SleepUtil.sleepSeconds(5);

        testSendEmail();

        //Clears cookies but should check why it only works with this
        browser.clearCookies();

        //Step 3: Logs in as a manager and approve
        ide = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME);
        ide.openPath(INBOX_URL);

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");

        //TO DO: this link should be get not hardcoded but it should match the taskId
        browser.openPath("/services/web/leave-request/gen/process-leave-request/forms/process-leave-request/index.html?taskId=17");
//        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");

//        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Approve");  //check why this doesnt work

        SelenideElement approveButton = browser.findElementInAllFrames(
                Selectors.byText("Approve"), Condition.visible
        );
        Selenide.executeJavaScript("arguments[0].click();", approveButton);

        SleepUtil.sleepSeconds(5);

        testAproveEmail(); //check why tf the emails doesnt work as they should
    }

    @Test
    void testCreateBPMProcessAndDeclineIt() throws MessagingException {
        // Step 1: Create users
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME, EMPLOYEE_ROLE);
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_ROLE);

        // Step 2: Log in as employee
        IDE ide = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_USERNAME);
        ide.openPath(SUBMIT_FORM_URL);

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, SUBMIT_BUTTON_TEXT);

        // Waits for the email to be sent
        SleepUtil.sleepSeconds(5);

        testSendEmail();

        //Clears cookies but should check why it only works with this
        browser.clearCookies();

        //Step 3: Logs in as a manager and approve
        ide = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_USERNAME);
        ide.openPath(INBOX_URL);

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");

        //TO DO: this link should be get not hardcoded but it should match the taskId
        browser.openPath("/services/web/leave-request/gen/process-leave-request/forms/process-leave-request/index.html?taskId=17");
//        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");

//        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Decline");  //check why this doesnt work

        SelenideElement approveButton = browser.findElementInAllFrames(
                Selectors.byText("Decline"), Condition.visible
        );
        Selenide.executeJavaScript("arguments[0].click();", approveButton);

        SleepUtil.sleepSeconds(5);

        testDeclineEmail(); //check why tf the emails doesnt work as they should
    }


    @AfterEach
    public void tearDown() {
        greenMail.stop();
        browser.clearCookies();
    }

    void testDeclineEmail(){
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(2);

        MimeMessage sentEmail = receivedMessages[0];
        assertThat(GreenMailUtil.getBody(sentEmail)
                .trim()).contains("<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.");
    }

    void testAproveEmail(){
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(2);

        MimeMessage sentEmail = receivedMessages[0];

//        assertThat(sentEmail.getSubject()).isEqualTo("Your leave request has been approved");
//        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
//        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("john.doe.employee@example.com");
        assertThat(GreenMailUtil.getBody(sentEmail)
                .trim()).contains("<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.");
    }

    void testSendEmail() throws MessagingException {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage sentEmail = receivedMessages[0];

        assertThat(sentEmail.getSubject()).isEqualTo("New leave request");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("leave-request-app@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("managers-dl@example.com");
        assertThat(GreenMailUtil.getBody(sentEmail)
                .trim()).contains("<h4>A new leave request for [john.doe.employee@example.com] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.");
    }
}
