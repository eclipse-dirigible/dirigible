/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.mail;

import jakarta.mail.MessagingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.dirigible.components.api.mail.MailClient;
import org.eclipse.dirigible.components.api.mail.MailFacade;

/**
 * SMTP delivery entry point. {@link #getInstance()} returns the platform-configured
 * {@link MailClient} (host, port, credentials picked up from {@code DIRIGIBLE_MAIL_*} config); the
 * {@link Properties}-accepting overload lets you override settings per call for one-off messages or
 * multi-tenant fan-out.
 * <p>
 * Each {@code send} accepts a list of {@code Map} parts — one per MIME body — that the underlying
 * client packs into a {@code multipart/mixed} or {@code multipart/alternative} structure. Use a
 * {@code text/plain} and {@code text/html} pair for typical transactional mail.
 */
public final class Mail {

    private Mail() {}

    public static MailClient getInstance() {
        return MailFacade.getInstance();
    }

    public static MailClient getInstance(Properties properties) {
        return MailFacade.getInstance(properties);
    }

    public static Map send(String from, String[] to, String[] cc, String[] bcc, String subject, List<Map> parts) throws MessagingException {
        return getInstance().send(from, to, cc, bcc, subject, parts);
    }
}
