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
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.integration.tests.ui.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.mail.EmailAsserter;
import org.eclipse.dirigible.tests.mail.EmailAssertion;
import org.eclipse.dirigible.tests.mail.EmailAssertionBuilder;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Lazy
@Component
class MailITTestProject extends BaseTestProject {

    static final String MAIL_USER = "mailUser";
    static final String MAIL_PASSWORD = "mailPassword";
    static final int MAIL_PORT = 56565;

    private final GreenMail greenMail;
    private final RestAssuredExecutor restAssuredExecutor;

    MailITTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, RestAssuredExecutor restAssuredExecutor) {
        super("MailIT", ide, projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
        this.greenMail = startGreenMailServer();
    }

    private GreenMail startGreenMailServer() {
        ServerSetup serverSetup = new ServerSetup(MAIL_PORT, "localhost", "smtp");
        GreenMail greenMail = new GreenMail(serverSetup);
        greenMail.start();
        greenMail.setUser(MAIL_USER, MAIL_PASSWORD);

        return greenMail;
    }

    @Override
    public void verify() throws Exception {
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

    @Override
    public void cleanup() {
        greenMail.stop();
    }
}
