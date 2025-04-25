/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class EmailAsserter {

    public static void assertReceivedEmailsCount(GreenMail greenMail, int expectedCount) {
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> greenMail.getReceivedMessages().length >= expectedCount);
    }

    public static void assertEmail(MimeMessage email, EmailAssertion emailAssertion) throws MessagingException {
        assertThat(email.getFrom()[0].toString()).isEqualTo(emailAssertion.from());
        assertThat(email.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(emailAssertion.to());
        assertThat(email.getSubject()).isEqualTo(emailAssertion.subject());

        assertBody(email, emailAssertion);

    }

    private static void assertBody(MimeMessage email, EmailAssertion emailAssertion) {
        String emailBody = GreenMailUtil.getBody(email)
                                        .trim();

        if (emailAssertion.toContainExactBody() != null) {
            assertThat(emailBody).contains(emailAssertion.toContainExactBody());
        }

        if (emailAssertion.bodyRegex() != null) {
            assertThat(emailBody).containsPattern(emailAssertion.bodyRegex());
        }
    }
}
