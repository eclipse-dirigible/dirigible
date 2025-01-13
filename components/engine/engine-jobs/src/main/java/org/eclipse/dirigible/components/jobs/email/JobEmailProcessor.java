/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.email;

import jakarta.mail.MessagingException;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.mail.MailFacade;
import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.eclipse.dirigible.components.engine.template.TemplateEnginesManager;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobEmail;
import org.eclipse.dirigible.components.jobs.service.JobEmailService;
import org.eclipse.dirigible.components.registry.accessor.RegistryAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class JobEmailProcessor.
 */
@Component
public class JobEmailProcessor {

    /** The Constant EMAIL_TEMPLATE_ERROR. */
    public static final String EMAIL_TEMPLATE_ERROR = "/job/templates/template-error.txt";
    /** The Constant EMAIL_TEMPLATE_NORMAL. */
    public static final String EMAIL_TEMPLATE_NORMAL = "/job/templates/template-normal.txt";
    /** The Constant EMAIL_TEMPLATE_ENABLE. */
    public static final String EMAIL_TEMPLATE_ENABLE = "/job/templates/template-enable.txt";
    /** The Constant EMAIL_TEMPLATE_DISABLE. */
    public static final String EMAIL_TEMPLATE_DISABLE = "/job/templates/template-disable.txt";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JobEmailProcessor.class);
    /** The Constant DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD. */
    private static final String DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD = "DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_SENDER. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_SENDER = "DIRIGIBLE_SCHEDULER_EMAIL_SENDER";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS = "DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR = "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL = "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ENABLE. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ENABLE = "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ENABLE";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_DISABLE. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_DISABLE = "DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_DISABLE";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR = "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL = "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ENABLE. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ENABLE = "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ENABLE";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_DISABLE. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_DISABLE = "DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_DISABLE";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME = "DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST = "DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST";
    /** The Constant DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT. */
    private static final String DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT = "DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT";
    /** The Constant DEFAULT_EMAIL_SUBJECT_ERROR. */
    private static final String DEFAULT_EMAIL_SUBJECT_ERROR = "Job execution failed: [%s]";
    /** The Constant DEFAULT_EMAIL_SUBJECT_NORMAL. */
    private static final String DEFAULT_EMAIL_SUBJECT_NORMAL = "Job execution is back to normal: [%s]";
    /** The Constant DEFAULT_EMAIL_SUBJECT_ENABLE. */
    private static final String DEFAULT_EMAIL_SUBJECT_ENABLE = "Job execution has been enabled: [%s]";
    /** The Constant DEFAULT_EMAIL_SUBJECT_DISABLE. */
    private static final String DEFAULT_EMAIL_SUBJECT_DISABLE = "Job execution has been disabled: [%s]";
    /** The email subject error. */
    public static String emailSubjectError = null;
    /** The email subject normal. */
    public static String emailSubjectNormal = null;
    /** The email subject enable. */
    public static String emailSubjectEnable = null;
    /** The email subject disable. */
    public static String emailSubjectDisable = null;
    /** The email template error. */
    public static String emailTemplateError = null;
    /** The email template normal. */
    public static String emailTemplateNormal = null;
    /** The email template enable. */
    public static String emailTemplateEnable = null;
    /** The email template disable. */
    public static String emailTemplateDisable = null;
    /** The logs retantion in hours. */
    private static int logsRetantionInHours = 24 * 7;
    /** The email sender. */
    private static String emailSender = null;
    /** The email recipients line. */
    private static String emailRecipientsLine = null;
    /** The email recipients. */
    private static String[] emailRecipients = null;
    /** The email url scheme. */
    private static String emailUrlScheme = null;
    /** The email url host. */
    private static String emailUrlHost = null;
    /** The email url port. */
    private static String emailUrlPort = null;

    static {
        try {
            logsRetantionInHours = Configuration.getAsInt(DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD, logsRetantionInHours);
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn(DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD
                        + " is not correctly set, so it will be backed up to a week timeframe (24x7)");
            }
            logsRetantionInHours = 24 * 7;
        }

        emailSender = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_SENDER);

        emailRecipientsLine = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS);
        if (emailRecipientsLine != null) {
            emailRecipients = emailRecipientsLine.split(",");
            for (String maybe : emailRecipients) {
                if (!EmailValidator.getInstance()
                                   .isValid(maybe)) {
                    emailRecipients = null;
                    if (logger.isWarnEnabled()) {
                        logger.warn(DIRIGIBLE_SCHEDULER_EMAIL_RECIPIENTS + " contains invalid e-mail address: " + maybe);
                    }
                    break;
                }
            }
        }

        emailSubjectError = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ERROR, DEFAULT_EMAIL_SUBJECT_ERROR);
        emailSubjectNormal = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_NORMAL, DEFAULT_EMAIL_SUBJECT_NORMAL);
        emailSubjectEnable = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_ENABLE, DEFAULT_EMAIL_SUBJECT_ENABLE);
        emailSubjectDisable = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_SUBJECT_DISABLE, DEFAULT_EMAIL_SUBJECT_DISABLE);
        emailTemplateError = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ERROR);
        emailTemplateNormal = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_NORMAL);
        emailTemplateEnable = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_ENABLE);
        emailTemplateDisable = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_TEMPLATE_DISABLE);
        emailUrlScheme = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_URL_SCHEME);
        emailUrlHost = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_URL_HOST);
        emailUrlPort = Configuration.get(DIRIGIBLE_SCHEDULER_EMAIL_URL_PORT);
    }

    /** The registry accessor. */
    @Autowired
    private RegistryAccessor registryAccessor;
    /** The job service. */
    @Autowired
    private JobEmailService jobEmailService;
    /** The generation engines manager. */
    @Autowired
    private TemplateEnginesManager generationEnginesManager;

    /**
     * Prepare email.
     *
     * @param job the job
     * @param templateLocation the template location
     * @param defaultLocation the default location
     * @return the string
     */
    public String prepareEmail(Job job, String templateLocation, String defaultLocation) {

        byte[] template = registryAccessor.getRegistryContent(templateLocation, defaultLocation);

        TemplateEngine generationEngine = generationEnginesManager.getTemplateEngine("mustache");
        if (generationEngine != null) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("job.name", job.getName());
            parameters.put("job.message", job.getMessage());
            parameters.put("job.scheme", emailUrlScheme);
            parameters.put("job.host", emailUrlHost);
            parameters.put("job.port", emailUrlPort);
            try {
                byte[] generated = generationEngine.generate(parameters, "~/temp", template);
                return new String(generated, StandardCharsets.UTF_8);
            } catch (IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error on generating the e-mail body: " + e.getMessage(), e);
                }
                return null;
            }
        }
        if (logger.isErrorEnabled()) {
            logger.error("Error on generating the e-mail body, because no template engine has been registered in this instance.");
        }
        return null;
    }

    /**
     * Send email.
     *
     * @param job the job
     * @param emailSubject the email subject
     * @param emailContent the email content
     */
    public void sendEmail(Job job, String emailSubject, String emailContent) {
        try {
            List<JobEmail> emailArtefacts = jobEmailService.findAllByJobName(job.getName());
            String[] emails = emailArtefacts.stream()
                                            .map(JobEmail::getEmail)
                                            .toArray(String[]::new);

            if (emailSender != null && ((emailRecipients != null && emailRecipients.length > 0) || emails.length > 0)) {

                List<Map> parts = new ArrayList<Map>();
                Map<String, String> map = new HashMap<>();
                map.put("contentType", ContentTypeHelper.TEXT_PLAIN);
                map.put("type", "text");
                map.put("text", emailContent);
                parts.add(map);
                MailFacade.getInstance()
                          .send(emailSender, emails.length > 0 ? emails : emailRecipients, null, null,
                                  String.format(emailSubject, job.getName()), parts);
                // String from, String[] to, String[] cc, String[] bcc, String subject, List<Map> parts
            } else {
                if (emailRecipientsLine != null) {
                    if (logger.isErrorEnabled()) {
                        logger.error("DIRIGIBLE_SCHEDULER_EMAIL_* environment variables are not set correctly");
                    }
                }
            }
        } catch (MessagingException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Sending an e-mail failed with: " + e.getMessage(), e);
            }
        }
    }

}
