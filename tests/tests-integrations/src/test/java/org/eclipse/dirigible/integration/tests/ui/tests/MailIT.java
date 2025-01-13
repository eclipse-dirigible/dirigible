/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.integration.tests.ui.TestProject;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.PortUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

class MailIT extends UserInterfaceIntegrationTest {

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final int PORT = PortUtil.getFreeRandomPort();

    static {
        Configuration.set("DIRIGIBLE_MAIL_USERNAME", USER);
        Configuration.set("DIRIGIBLE_MAIL_PASSWORD", PASSWORD);
        Configuration.set("DIRIGIBLE_MAIL_TRANSPORT_PROTOCOL", "smtp");
        Configuration.set("DIRIGIBLE_MAIL_SMTP_HOST", "localhost");
        Configuration.set("DIRIGIBLE_MAIL_SMTP_PORT", Integer.toString(PORT));
        Configuration.set("DIRIGIBLE_MAIL_SMTP_AUTH", "true");
    }

    @Autowired
    private TestProject testProject;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        ServerSetup serverSetup = new ServerSetup(PORT, "localhost", "smtp");
        greenMail = new GreenMail(serverSetup);

        greenMail.start();

        greenMail.setUser(USER, PASSWORD);

        testProject.publish();
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    void testSendEmail() throws MessagingException {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ts/dirigible-test-project/mail/MailService.ts/sendTestEmail")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Mail has been sent")));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage sentEmail = receivedMessages[0];

        assertThat(sentEmail.getSubject()).isEqualTo("A test email");
        assertThat(sentEmail.getFrom()[0].toString()).isEqualTo("from@example.com");
        assertThat(sentEmail.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("to@example.com");
        assertThat(GreenMailUtil.getBody(sentEmail)
                                .trim()).contains("<h2>Test email content</h2>");

    }

}
