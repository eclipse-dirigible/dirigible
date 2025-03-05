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
import org.eclipse.dirigible.tests.mail.EmailAsserter;
import org.eclipse.dirigible.tests.mail.EmailAssertion;
import org.eclipse.dirigible.tests.mail.EmailAssertionBuilder;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.PortUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

class MailIT extends UserInterfaceIntegrationTest {

    private static final String USER = "emailUser";
    private static final String PASSWORD = "emailPassword";
    private static final int PORT = PortUtil.getFreeRandomPort();

    static {
        configureEmail();
    }

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private GreenMail greenMail;

    private static void configureEmail() {
        DirigibleConfig.MAIL_USERNAME.setStringValue(USER);
        DirigibleConfig.MAIL_PASSWORD.setStringValue(PASSWORD);
        DirigibleConfig.MAIL_TRANSPORT_PROTOCOL.setStringValue("smtp");
        DirigibleConfig.MAIL_SMTP_HOST.setStringValue("localhost");
        DirigibleConfig.MAIL_SMTP_PORT.setIntValue(PORT);
        DirigibleConfig.MAIL_SMTP_AUTH.setBooleanValue(true);
    }

    @BeforeEach
    void setUp() {
        startEmailMock();
    }

    private void startEmailMock() {
        ServerSetup serverSetup = new ServerSetup(PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);

        greenMail.start();

        greenMail.setUser(USER, PASSWORD);
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    void testSendEmail() throws MessagingException {
        ide.createAndPublishProjectFromResources("MailIT");

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ts/MailIT/mail/MailService.ts/sendTestEmail")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Mail has been sent")));

        EmailAssertion emailAssertion = new EmailAssertionBuilder().expectedFrom("from@example.com")
                                                                   .expectedTo("to@example.com")
                                                                   .expectedSubject("A test email")
                                                                   .expectedToContainBody("<h2>Test email content</h2>")
                                                                   .build();

        EmailAsserter.assertReceivedEmailsCount(greenMail, 1);
        MimeMessage receivedEmail = greenMail.getReceivedMessages()[0];

        EmailAsserter.assertEmail(receivedEmail, emailAssertion);
    }

}
