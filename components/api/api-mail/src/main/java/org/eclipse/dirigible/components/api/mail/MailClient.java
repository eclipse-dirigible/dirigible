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

import com.google.gson.Gson;
import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.eclipse.angus.mail.smtp.SMTPSSLTransport;
import org.eclipse.angus.mail.smtp.SMTPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * The Class MailClient.
 */
public class MailClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailClient.class);

    /** The Constant MAIL_USER. */
    // Mail properties
    private static final String MAIL_USER = "mail.user";
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    /** The Constant MAIL_PASSWORD. */
    private static final String MAIL_PASSWORD = "mail.password";
    /** The Constant SMTP_TRANSPORT. */
    private static final String SMTP_TRANSPORT = "smtp";
    /** The Constant SMTPS_TRANSPORT. */
    private static final String SMTPS_TRANSPORT = "smtps";
    /** The properties. */
    private final Properties properties;

    /**
     * Instantiates a new mail client.
     *
     * @param properties mail client configuration options
     */
    public MailClient(Properties properties) {
        this.properties = properties;
    }

    /**
     * Send an email.
     *
     * @param from the sender
     * @param to the to receiver
     * @param cc the cc receiver
     * @param bcc the bcc receiver
     * @param subject the subject
     * @param parts the mail parts
     * @return the map
     * @throws MessagingException the messaging exception
     */
    public Map send(String from, String[] to, String[] cc, String[] bcc, String subject, List<Map> parts) throws MessagingException {
        try {
            Session session = getSession(this.properties);
            SMTPTransport transport;
            String protocol = properties.getProperty(MAIL_TRANSPORT_PROTOCOL);
            if (null == protocol) {
                throw new IllegalStateException("Missing property " + MAIL_TRANSPORT_PROTOCOL);
            }
            String transportProperty = protocol.toLowerCase();

            transport = switch (transportProperty) {
                case SMTP_TRANSPORT -> (SMTPTransport) session.getTransport();
                case SMTPS_TRANSPORT -> (SMTPSSLTransport) session.getTransport();
                default -> throw new IllegalStateException("Unexpected transport property: " + transportProperty);
            };

            try {
                String proxyType = this.properties.getProperty("ProxyType");
                if (proxyType != null && proxyType.equals("OnPremise")) {
                    Socket socket = new ConnectivitySocks5ProxySocket(getTransportProperty(transportProperty, "socks.host"),
                            getTransportProperty(transportProperty, "socks.port"), getTransportProperty(transportProperty, "proxy.user"),
                            getTransportProperty(transportProperty, "proxy.password", " "));

                    socket.connect(new InetSocketAddress(getTransportProperty(transportProperty, "host"),
                            Integer.parseInt(getTransportProperty(transportProperty, "port"))));

                    transport.connect(socket);
                } else {
                    transport.connect(this.properties.getProperty(MAIL_USER), this.properties.getProperty(MAIL_PASSWORD));
                }

                MimeMessage mimeMessage = createMimeMessage(session, from, to, cc, bcc, subject, parts);
                mimeMessage.saveChanges();
                String messageId = mimeMessage.getMessageID();
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                String finalReply = transport.getLastServerResponse();
                Map mailResult = new HashMap();
                mailResult.put("messageId", messageId);
                mailResult.put("finalReply", finalReply);

                return mailResult;
            } finally {
                transport.close();
            }
        } catch (MessagingException | IOException | RuntimeException ex) {
            String message = "Failed to send email from [" + from + "] to " + Arrays.toString(to);
            LOGGER.error(message, ex); // log the message since the js may not log it properly
            throw new MessagingException(message, ex);
        }

    }

    /**
     * Gets the session.
     *
     * @param properties the properties
     * @return the session
     */
    private Session getSession(Properties properties) {
        String user = properties.getProperty(MAIL_USER);
        String password = properties.getProperty(MAIL_PASSWORD);
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        };
        return Session.getInstance(properties, authenticator);
    }

    /**
     * Creates the mime message.
     *
     * @param smtpSession the smtp session
     * @param from the from
     * @param to the to
     * @param cc the cc
     * @param bcc the bcc
     * @param subjectText the subject text
     * @param parts the parts
     * @return the mime message
     * @throws MessagingException the messaging exception
     */
    static MimeMessage createMimeMessage(Session smtpSession, String from, String[] to, String[] cc, String[] bcc, String subjectText,
            List<Map> parts) throws MessagingException {

        MimeMessage mimeMessage = new MimeMessage(smtpSession);
        mimeMessage.setFrom(InternetAddress.parse(from)[0]);
        for (String next : to) {
            mimeMessage.addRecipients(Message.RecipientType.TO, InternetAddress.parse(next));
        }
        if (cc != null) {
            for (String next : cc) {
                mimeMessage.addRecipients(Message.RecipientType.CC, InternetAddress.parse(next));
            }
        }
        if (bcc != null) {
            for (String next : bcc) {
                mimeMessage.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(next));
            }
        }
        mimeMessage.setSubject(subjectText, "UTF-8"); //$NON-NLS-1$

        MimeMultipart multiPart = new MimeMultipart("mixed");
        List<MimeBodyPart> plainTextParts = new ArrayList<>();
        List<MimeBodyPart> htmlTextParts = new ArrayList<>();
        int textPosition = -1;

        for (Map mailPart : parts) {
            String type = (String) mailPart.get("type");
            ContentType contentType;
            String contentId;
            String fileName;
            byte[] dataBytes;
            ByteArrayDataSource source;

            switch (type) {
                case "text":
                    contentType = new ContentType((String) mailPart.get("contentType"));
                    String mailText = (String) mailPart.get("text");

                    switch (contentType.getSubType()) {
                        case "plain":
                            MimeBodyPart plainTextPart = new MimeBodyPart();
                            plainTextPart.setText(mailText, "utf-8", contentType.getSubType());
                            plainTextParts.add(plainTextPart);
                            break;
                        case "html":
                            MimeBodyPart htmlTextPart = new MimeBodyPart();
                            // UTF-8 unless the caller named a charset: interpolated business data is
                            // routinely non-ASCII, and a text/html part without one is read as ASCII.
                            String charset = contentType.getParameter("charset");
                            htmlTextPart.setText(mailText, charset == null ? "utf-8" : charset, contentType.getSubType());
                            htmlTextParts.add(htmlTextPart);
                            break;
                    }
                    if (textPosition < 0) {
                        textPosition = multiPart.getCount();
                    }
                    break;
                case "inline":
                    contentType = new ContentType((String) mailPart.get("contentType"));
                    contentId = (String) mailPart.get("contentId");
                    fileName = (String) mailPart.get("fileName");
                    dataBytes = partBytes(mailPart.get("data"));

                    MimeBodyPart inlinePart = new MimeBodyPart();
                    source = new ByteArrayDataSource(dataBytes, String.valueOf(contentType));
                    inlinePart.setDataHandler(new DataHandler(source));
                    inlinePart.setContentID("<" + contentId + ">");
                    inlinePart.setDisposition(MimeBodyPart.INLINE);
                    inlinePart.setFileName(fileName);

                    multiPart.addBodyPart(inlinePart);
                    break;
                case "attachment":
                    contentType = new ContentType((String) mailPart.get("contentType"));
                    fileName = (String) mailPart.get("fileName");
                    dataBytes = partBytes(mailPart.get("data"));

                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    source = new ByteArrayDataSource(dataBytes, String.valueOf(contentType));
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(fileName);

                    multiPart.addBodyPart(attachmentPart);
                    break;
            }
        }

        if (textPosition >= 0) {
            addTextParts(multiPart, textPosition, plainTextParts, htmlTextParts);
        }
        mimeMessage.setContent(multiPart);

        return mimeMessage;
    }

    /**
     * Places the message's text bodies where the first of them was given. A plain and an HTML body are
     * the same message in two renderings, so when both are present they go into one
     * {@code multipart/alternative} - plain first, HTML last, the order RFC 2046 reads as increasing
     * preference - which then sits in the {@code mixed} container beside the attachments. As two
     * siblings of the {@code mixed} container several clients show them as two bodies, one after the
     * other. A single rendering is added as it always was.
     *
     * @param multiPart the {@code mixed} container
     * @param position the index the text bodies take in it
     * @param plainTextParts the {@code text/plain} bodies, in the given order
     * @param htmlTextParts the {@code text/html} bodies, in the given order
     * @throws MessagingException if a part cannot be added
     */
    private static void addTextParts(MimeMultipart multiPart, int position, List<MimeBodyPart> plainTextParts,
            List<MimeBodyPart> htmlTextParts) throws MessagingException {
        if (plainTextParts.isEmpty() || htmlTextParts.isEmpty()) {
            int index = position;
            for (MimeBodyPart textPart : plainTextParts.isEmpty() ? htmlTextParts : plainTextParts) {
                multiPart.addBodyPart(textPart, index++);
            }
            return;
        }
        MimeMultipart alternative = new MimeMultipart("alternative");
        for (MimeBodyPart textPart : plainTextParts) {
            alternative.addBodyPart(textPart);
        }
        for (MimeBodyPart textPart : htmlTextParts) {
            alternative.addBodyPart(textPart);
        }
        MimeBodyPart alternativePart = new MimeBodyPart();
        alternativePart.setContent(alternative);
        multiPart.addBodyPart(alternativePart, position);
    }

    /**
     * The bytes of an {@code inline} / {@code attachment} part's {@code data}. A JavaScript caller
     * passes the byte array as a JSON array string (the historical shape, kept working); a Java caller
     * holding the bytes already - e.g. a server-side render of a document to PDF - passes the
     * {@code byte[]} itself, so a binary payload does not have to travel through a JSON int array.
     *
     * @param data the part's data value: a {@code byte[]}, or a JSON array string of numbers
     * @return the decoded bytes
     */
    private static byte[] partBytes(Object data) {
        if (data instanceof byte[] bytes) {
            return bytes;
        }
        return new Gson().fromJson((String) data, byte[].class);
    }

    /**
     * Gets the transport property.
     *
     * @param transport the transport
     * @param prop the prop
     * @return the transport property
     */
    private String getTransportProperty(String transport, String prop) {
        return this.properties.getProperty("mail." + transport + "." + prop);
    }

    /**
     * Gets the transport property.
     *
     * @param transport the transport
     * @param prop the prop
     * @param defaultValue the default value
     * @return the transport property
     */
    private String getTransportProperty(String transport, String prop, String defaultValue) {
        return this.properties.getProperty("mail." + transport + "." + prop, defaultValue);
    }

}
