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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;

/**
 * An {@code attachment} part's {@code data} may arrive as raw {@code byte[]} (a Java caller that
 * already holds the bytes - e.g. a document rendered to PDF server-side) or as the historical JSON
 * array string a JavaScript caller sends. Both must reach the MIME part byte-identically: a binary
 * payload forced through a JSON int array is what the {@code byte[]} form exists to avoid.
 */
class MailClientAttachmentTest {

    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34, (byte) 0x80, 0x00, (byte) 0xff};

    @Test
    void attachmentAcceptsRawBytes() throws Exception {
        MimeMessage message = message(part(PDF));

        assertEquals("invoice.pdf", attachment(message).getFileName());
        assertArrayEquals(PDF, attachmentBytes(message));
    }

    @Test
    void attachmentStillAcceptsTheJsonArrayString() throws Exception {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < PDF.length; i++) {
            json.append(i == 0 ? "" : ",")
                .append(PDF[i]);
        }
        MimeMessage message = message(part(json.append("]")
                                               .toString()));

        assertArrayEquals(PDF, attachmentBytes(message));
    }

    private static Map part(Object data) {
        return Map.of("type", "attachment", "contentType", "application/pdf", "fileName", "invoice.pdf", "data", data);
    }

    private static MimeMessage message(Map attachmentPart) throws Exception {
        List<Map> parts = new java.util.ArrayList<>();
        parts.add(Map.of("type", "text", "contentType", "text/plain", "text", "see attached"));
        parts.add(attachmentPart);
        return MailClient.createMimeMessage(Session.getInstance(new Properties()), "from@example.com", new String[] {"to@example.com"},
                new String[0], new String[0], "Invoice", parts);
    }

    private static jakarta.mail.Part attachment(MimeMessage message) throws Exception {
        return ((MimeMultipart) message.getContent()).getBodyPart(1);
    }

    private static byte[] attachmentBytes(MimeMessage message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = attachment(message).getInputStream()) {
            in.transferTo(out);
        }
        return out.toByteArray();
    }
}
