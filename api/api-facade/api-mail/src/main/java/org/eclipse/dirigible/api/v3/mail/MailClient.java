/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.v3.mail;

import com.google.gson.Gson;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MailClient {
	// Mail properties
	private static final String MAIL_USER = "mail.user";
	private static final String MAIL_PASSWORD = "mail.password";
	private static final String SMTP_TRANSPORT = "smtp";
	private static final String SMTPS_TRANSPORT = "smtps";

	private final Properties properties;

	/**
	 * @param properties mail client configuration options
	 */
	public MailClient(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Send an email
	 *
	 * @param from    the sender
	 * @param to      the to receiver
	 * @param cc      the cc receiver
	 * @param bcc     the bcc receiver
	 * @param subject the subject
	 * @param parts   the mail parts
	 * @throws MessagingException
	 */
	public Map send(String from, String[] to, String[] cc, String[] bcc, String subject, List<Map> parts) throws MessagingException, IOException {
		Session session = getSession(this.properties);
		SMTPTransport transport;
		String transportProperty = properties.getProperty("mail.transport.protocol").toLowerCase();

		switch (transportProperty) {
			case SMTP_TRANSPORT:
				transport = (SMTPTransport) session.getTransport();
				break;
			case SMTPS_TRANSPORT:
				transport = (SMTPSSLTransport) session.getTransport();
				break;
			default:
				throw new IllegalStateException("Unexpected transport property: " + transportProperty);
		}

		if (this.properties.getProperty("ProxyType").equals("OnPremise")) {
			Socket socket =
					new ConnectivitySocks5ProxySocket(getTransportProperty(transportProperty, "socks.host"),
							getTransportProperty(transportProperty, "socks.port"),
							getTransportProperty(transportProperty, "proxy.user"),
							getTransportProperty(transportProperty, "proxy.password", " "));

			socket.connect(new InetSocketAddress(getTransportProperty(transportProperty, "host"),
					Integer.parseInt(getTransportProperty(transportProperty, "port"))));

			transport.connect(socket);
		} else {
			transport.connect();
		}


		MimeMessage mimeMessage = createMimeMessage(session, from, to, cc, bcc, subject, parts);
		mimeMessage.saveChanges();
		String messageId = mimeMessage.getMessageID();
		transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
		String finalReply = transport.getLastServerResponse();
		transport.close();

		Map mailResult = new HashMap();
		mailResult.put("messageId", messageId);
		mailResult.put("finalReply", finalReply);

		return mailResult;
	}

	private Session getSession(Properties properties) {
		String user = properties.getProperty(MAIL_USER);
		String password = properties.getProperty(MAIL_PASSWORD);
		Authenticator authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, password);
			}
		};
		return Session.getInstance(properties, authenticator);
	}

	private static MimeMessage createMimeMessage(Session smtpSession, String from, String to[], String cc[], String bcc[], String subjectText, List<Map> parts)
			throws MessagingException {

		MimeMessage mimeMessage = new MimeMessage(smtpSession);
		mimeMessage.setFrom(InternetAddress.parse(from)[0]);
		for (String next : to) {
			mimeMessage.addRecipients(Message.RecipientType.TO, InternetAddress.parse(next));
		}
		for (String next : cc) {
			mimeMessage.addRecipients(Message.RecipientType.CC, InternetAddress.parse(next));
		}
		for (String next : bcc) {
			mimeMessage.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(next));
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

	private String getTransportProperty(String transport, String prop) {
		return this.properties.getProperty("mail." + transport + "." + prop);
	}

	private String getTransportProperty(String transport, String prop, String defaultValue) {
		return this.properties.getProperty("mail." + transport + "." + prop, defaultValue);
	}
}
