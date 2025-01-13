/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
    private static MimeMessage createMimeMessage(Session smtpSession, String from, String[] to, String[] cc, String[] bcc,
            String subjectText, List<Map> parts) throws MessagingException {

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

        for (Map mailPart : parts) {
            String type = (String) mailPart.get("type");
            ContentType contentType;
            String contentId;
            String fileName;
            String data;
            Gson gson = new Gson();
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
                            multiPart.addBodyPart(plainTextPart);
                            break;
                        case "html":
                            MimeBodyPart htmlTextPart = new MimeBodyPart();
                            htmlTextPart.setContent(mailText, String.valueOf(contentType));
                            multiPart.addBodyPart(htmlTextPart);
                            break;
                    }
                    break;
                case "inline":
                    contentType = new ContentType((String) mailPart.get("contentType"));
                    contentId = (String) mailPart.get("contentId");
                    fileName = (String) mailPart.get("fileName");
                    data = (String) mailPart.get("data");

                    dataBytes = gson.fromJson(data, byte[].class);

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
                    data = (String) mailPart.get("data");

                    dataBytes = gson.fromJson(data, byte[].class);

                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    source = new ByteArrayDataSource(dataBytes, String.valueOf(contentType));
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(fileName);

                    multiPart.addBodyPart(attachmentPart);
                    break;
            }
        }

        mimeMessage.setContent(multiPart);

        return mimeMessage;
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
