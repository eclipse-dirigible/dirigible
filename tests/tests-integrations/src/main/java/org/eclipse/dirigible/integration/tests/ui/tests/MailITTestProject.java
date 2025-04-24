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
import jakarta.mail.internet.MimeMessage;
import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.mail.EmailAsserter;
import org.eclipse.dirigible.tests.framework.mail.EmailAssertion;
import org.eclipse.dirigible.tests.framework.mail.EmailAssertionBuilder;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Lazy
@Component
class MailITTestProject extends BaseTestProject {

    private final GreenMail greenMail;
    private final RestAssuredExecutor restAssuredExecutor;

    MailITTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, RestAssuredExecutor restAssuredExecutor, GreenMail greenMail) {
        super("MailIT", ide, projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
        this.greenMail = greenMail;
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
}
