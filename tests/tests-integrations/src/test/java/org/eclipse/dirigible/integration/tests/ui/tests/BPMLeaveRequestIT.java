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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.IDEFactory;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.HtmlElementType;
import org.eclipse.dirigible.tests.mail.EmailAsserter;
import org.eclipse.dirigible.tests.mail.EmailAssertion;
import org.eclipse.dirigible.tests.mail.EmailAssertionBuilder;
import org.eclipse.dirigible.tests.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

class BPMLeaveRequestIT extends UserInterfaceIntegrationTest {

    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_PASSWORD = "employeePassword";
    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String EMPLOYEE_MANAGER_PASSWORD = "managerPassword";
    private static final String MAIL_USER = "mailUser";
    private static final String MAIL_PASSWORD = "mailPassword";
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
        browser.close();

        createTestUsers();

        submitLeaveRequest();

        assertNotificationEmailReceived();
    }

    private void startGreenMailServer() {
        ServerSetup serverSetup = new ServerSetup(MAIL_PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);
        greenMail.start();
        greenMail.setUser(MAIL_USER, MAIL_PASSWORD);
    }

    private void submitLeaveRequest() {
        IDE employeeIde = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        employeeIde.openPath("/services/web/" + testProject.getProjectResourcesFolder()
                + "/gen/submit-leave-request/forms/submit-leave-request/index.html");
        fillLeaveRequestForm(employeeIde.getBrowser());

        employeeIde.getBrowser()
                   .assertAlertWithMessage("Leave request has been created.");

        employeeIde.close();
    }

    private void fillLeaveRequestForm(Browser browser) {
        browser.enterTextInElementById("fromId", "02/02/2002");
        browser.enterTextInElementById("toId", "03/03/2002");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Submit");
    }

    private void createTestUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, "employee");
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD, "employee-manager");
    }

    private void assertNotificationEmailReceived() throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 1);
        MimeMessage receivedEmail = getLatestReceivedEmailMessage();

        EmailAssertion emailAssertion = new EmailAssertionBuilder().expectedFrom("leave-request-app@example.com")
                                                                   .expectedTo("managers-dl@example.com")
                                                                   .expectedSubject("New leave request")
                                                                   .expectedToContainBody("<h4>A new leave request for ["
                                                                           + EMPLOYEE_USERNAME
                                                                           + "] has been created</h4>Open the inbox <a href=\"http://localhost:80/services/web/inbox/\" target=\"_blank\">here</a> to process the request.")
                                                                   .build();
        EmailAsserter.assertEmail(receivedEmail, emailAssertion);
    }

    private MimeMessage getLatestReceivedEmailMessage() {
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        return receivedMessages[receivedMessages.length - 1];
    }

    @Test
    void testApproveLeaveRequest() throws MessagingException {
        boolean approve = true;
        processRequest(approve);

        assertLeaveRequestEmail(approve);
    }

    private void processRequest(boolean approve) {
        IDE managerIDE = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD);

        claimRequest(managerIDE);

        Browser managerBrowser = managerIDE.getBrowser();

        String buttonLabel = approve ? "Approve" : "Decline";
        managerBrowser.clickOnElementContainingText(HtmlElementType.BUTTON, buttonLabel);

        String alertMessage = approve ? "Request Approved" : "Request Declined";
        managerBrowser.assertAlertWithMessage(alertMessage);
    }

    private void claimRequest(IDE managerIDE) {
        managerIDE.openPath("/services/web/inbox/");

        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Close");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");
        browser.switchToLatestTab();
    }

    private void assertLeaveRequestEmail(boolean approve) throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 2);

        MimeMessage receivedEmail = getLatestReceivedEmailMessage();

        String decision = approve ? "approved" : "declined";
        String bodyRegex = "<h4>Your leave request from \\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z] to "
                + "\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z] has been " + decision + " by \\["
                + Pattern.quote(EMPLOYEE_MANAGER_USERNAME) + "]</h4>";

        EmailAssertion emailAssertion = new EmailAssertionBuilder().expectedFrom("leave-request-app@example.com")
                                                                   .expectedTo(EMPLOYEE_USERNAME)
                                                                   .expectedSubject("Your leave request has been " + decision)
                                                                   .expectedBodyRegex(bodyRegex)
                                                                   .build();
        EmailAsserter.assertEmail(receivedEmail, emailAssertion);
    }

    @Test
    void testDeclineLeaveRequest() throws MessagingException {
        boolean approve = false;
        processRequest(approve);

        assertLeaveRequestEmail(approve);
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }
}
