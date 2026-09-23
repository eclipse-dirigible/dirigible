/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;

/**
 * A plain and an HTML body are one message in two renderings (dirigible #7488): they must travel as
 * one {@code multipart/alternative}, not as two siblings of the {@code mixed} container, which
 * several clients show as two bodies one after the other.
 */
class MailClientAlternativeTest {

    private static final Map PLAIN = Map.of("type", "text", "contentType", "text/plain", "text", "Hello Иван");
    private static final Map HTML = Map.of("type", "text", "contentType", "text/html", "text", "<p>Hello <b>Иван</b></p>");
    private static final Map PDF = Map.of("type", "attachment", "contentType", "application/pdf", "fileName", "invoice.pdf", "data",
            new byte[] {0x25, 0x50, 0x44, 0x46});

    @Test
    void plainAndHtmlBecomeOneAlternativeBesideTheAttachment() throws Exception {
        MimeMultipart mixed = content(PLAIN, HTML, PDF);

        assertTrue(mixed.getContentType()
                        .startsWith("multipart/mixed"),
                mixed.getContentType());
        assertEquals(2, mixed.getCount());
        MimeMultipart alternative = (MimeMultipart) mixed.getBodyPart(0)
                                                         .getContent();
        assertTrue(alternative.getContentType()
                              .startsWith("multipart/alternative"),
                alternative.getContentType());
        assertEquals(2, alternative.getCount());
        assertTrue(alternative.getBodyPart(0)
                              .isMimeType("text/plain"),
                "the plain rendering comes first - the lowest preference");
        assertTrue(alternative.getBodyPart(1)
                              .isMimeType("text/html"),
                "the html rendering comes last - the highest preference");
        assertEquals("Hello Иван", alternative.getBodyPart(0)
                                              .getContent());
        assertEquals("<p>Hello <b>Иван</b></p>", alternative.getBodyPart(1)
                                                            .getContent());
        assertEquals("invoice.pdf", mixed.getBodyPart(1)
                                         .getFileName());
    }

    @Test
    void theAlternativeTakesThePlaceOfTheFirstTextBody() throws Exception {
        MimeMultipart mixed = content(PDF, PLAIN, HTML);

        assertEquals("invoice.pdf", mixed.getBodyPart(0)
                                         .getFileName());
        assertTrue(mixed.getBodyPart(1)
                        .isMimeType("multipart/alternative"));
    }

    @Test
    void aSinglePlainBodyIsAddedAsItAlwaysWas() throws Exception {
        MimeMultipart mixed = content(PLAIN, PDF);

        assertEquals(2, mixed.getCount());
        BodyPart body = mixed.getBodyPart(0);
        assertTrue(body.isMimeType("text/plain"));
        assertEquals("Hello Иван", body.getContent());
    }

    @Test
    void aSingleHtmlBodyIsAddedOnItsOwnInUtf8() throws Exception {
        MimeMultipart mixed = content(HTML);

        assertEquals(1, mixed.getCount());
        BodyPart body = mixed.getBodyPart(0);
        assertTrue(body.isMimeType("text/html"));
        assertTrue(body.getContentType()
                       .toLowerCase()
                       .contains("charset=utf-8"),
                body.getContentType());
        assertEquals("<p>Hello <b>Иван</b></p>", body.getContent());
    }

    private static MimeMultipart content(Map... parts) throws Exception {
        List<Map> list = new ArrayList<>(List.of(parts));
        MimeMessage message = MailClient.createMimeMessage(Session.getInstance(new Properties()), "from@example.com",
                new String[] {"to@example.com"}, new String[0], new String[0], "Hello", list);
        message.saveChanges();
        return (MimeMultipart) message.getContent();
    }
}
