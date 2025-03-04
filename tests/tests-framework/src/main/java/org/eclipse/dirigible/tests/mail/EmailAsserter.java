/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class EmailAsserter {

    public static void assertReceivedEmailsCount(GreenMail greenMail, int expectedCount) {
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> greenMail.getReceivedMessages().length >= expectedCount);
    }

    public static void assertEmailReceived(MimeMessage email, String expectedSubject, String expectedContent, String expectedFrom,
            String expectedTo) throws MessagingException {
        assertThat(email.getSubject()).isEqualTo(expectedSubject);
        assertThat(email.getFrom()[0].toString()).isEqualTo(expectedFrom);
        assertThat(email.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(expectedTo);

        String emailBody = GreenMailUtil.getBody(email)
                                        .trim();

        String extractedFromDate = extractDateFromBody(emailBody, "from \\[");
        String extractedToDate = extractDateFromBody(emailBody, "to \\[");

        if (!extractedFromDate.isEmpty() && !extractedToDate.isEmpty()) {
            assertThat(extractedFromDate).isEqualTo("2002-02-02");
            assertThat(extractedToDate).isEqualTo("2002-03-03");
        } else {
            assertThat(emailBody).contains(expectedContent);
        }
    }

    private static String extractDateFromBody(String emailBody, String pattern) {
        Pattern regexPattern = Pattern.compile(pattern + "(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = regexPattern.matcher(emailBody);
        return matcher.find() ? matcher.group(1) : "";
    }
}
