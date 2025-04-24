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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.tests.framework.browser.Browser;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.mail.EmailAsserter;
import org.eclipse.dirigible.tests.framework.mail.EmailAssertion;
import org.eclipse.dirigible.tests.framework.mail.EmailAssertionBuilder;
import org.eclipse.dirigible.tests.framework.projects.BaseTestProject;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Lazy
@Component
abstract class BPMLeaveRequestTestProject extends BaseTestProject {

    private static final String EMPLOYEE_USERNAME = "john.doe.employee@example.com";
    private static final String EMPLOYEE_PASSWORD = "employeePassword";

    private static final String EMPLOYEE_MANAGER_USERNAME = "emily.stone.mngr@example.com";
    private static final String EMPLOYEE_MANAGER_PASSWORD = "managerPassword";

    private static final String PROCESS_LEAVE_REQUEST_FORM_FILENAME = "process-leave-request.form";
    private static final String SUBMIT_LEAVE_REQUEST_FORM_FILENAME = "submit-leave-request.form";

    private final GreenMail greenMail;
    private final SecurityUtil securityUtil;
    private final IDEFactory ideFactory;

    BPMLeaveRequestTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, SecurityUtil securityUtil, IDEFactory ideFactory,
            GreenMail greenMail) {
        super("BPMLeaveRequestIT", ide, projectUtil, edmView);
        this.securityUtil = securityUtil;
        this.ideFactory = ideFactory;
        this.greenMail = greenMail;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        generateForms(getProjectResourcesFolder(), PROCESS_LEAVE_REQUEST_FORM_FILENAME, SUBMIT_LEAVE_REQUEST_FORM_FILENAME);

        publish();
        getIde().close();

        createTestUsers();
    }

    private void createTestUsers() {
        securityUtil.createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, "employee");
        securityUtil.createUser(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD, "employee-manager");
    }

    @Override
    public void verify() throws Exception {
        submitLeaveRequest();
        assertNotificationEmailReceived();

        processRequest();

        assertLeaveRequestEmail();
    }

    private void submitLeaveRequest() {
        IDE employeeIde = ideFactory.create(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        employeeIde.openPath(
                "/services/web/" + getProjectResourcesFolder() + "/gen/submit-leave-request/forms/submit-leave-request/index.html");
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

    private void processRequest() {
        IDE managerIDE = ideFactory.create(EMPLOYEE_MANAGER_USERNAME, EMPLOYEE_MANAGER_PASSWORD);

        claimRequest(managerIDE);

        Browser managerBrowser = managerIDE.getBrowser();

        boolean approve = shouldApproveRequest();
        String buttonLabel = approve ? "Approve" : "Decline";
        managerBrowser.clickOnElementContainingText(HtmlElementType.BUTTON, buttonLabel);

        String alertMessage = approve ? "Request Approved" : "Request Declined";
        managerBrowser.assertAlertWithMessage(alertMessage);

        managerIDE.close();
    }

    protected abstract boolean shouldApproveRequest();

    private void claimRequest(IDE managerIDE) {
        managerIDE.openPath("/services/web/inbox/");

        Browser browser = managerIDE.getBrowser();
        browser.clickOnElementContainingText(HtmlElementType.TR, "Process request");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Claim");

        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Open Form");
        browser.switchToLatestTab();
    }

    private void assertLeaveRequestEmail() throws MessagingException {
        EmailAsserter.assertReceivedEmailsCount(greenMail, 2);

        MimeMessage receivedEmail = getLatestReceivedEmailMessage();

        String decision = shouldApproveRequest() ? "approved" : "declined";
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
}
