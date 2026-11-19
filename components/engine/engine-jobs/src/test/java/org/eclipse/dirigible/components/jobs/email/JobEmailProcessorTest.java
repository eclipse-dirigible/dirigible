/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.engine.template.TemplateEnginesManager;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobEmail;
import org.eclipse.dirigible.components.jobs.service.JobEmailService;
import org.eclipse.dirigible.components.registry.accessor.RegistryAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * A job's status change renders its e-mail only when one is due (#8704).
 */
class JobEmailProcessorTest {

    private static final String JOB_NAME = "/project/nightly.job";

    private static final String NO_TEMPLATE_ENGINE =
            "Error on generating the e-mail body, because no template engine has been registered in this instance.";

    private final RegistryAccessor registryAccessor = mock(RegistryAccessor.class);

    private final JobEmailService jobEmailService = mock(JobEmailService.class);

    private final TemplateEnginesManager templateEnginesManager = mock(TemplateEnginesManager.class);

    private final JobEmailProcessor processor = new JobEmailProcessor();

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private final Logger logger = (Logger) LoggerFactory.getLogger(JobEmailProcessor.class);

    private Object originalSender;

    private Object originalRecipientsLine;

    private Object originalRecipients;

    @BeforeEach
    void setUp() {
        originalSender = ReflectionTestUtils.getField(JobEmailProcessor.class, "emailSender");
        originalRecipientsLine = ReflectionTestUtils.getField(JobEmailProcessor.class, "emailRecipientsLine");
        originalRecipients = ReflectionTestUtils.getField(JobEmailProcessor.class, "emailRecipients");
        configure(null, null);

        ReflectionTestUtils.setField(processor, "registryAccessor", registryAccessor);
        ReflectionTestUtils.setField(processor, "jobEmailService", jobEmailService);
        ReflectionTestUtils.setField(processor, "generationEnginesManager", templateEnginesManager);

        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailSender", originalSender);
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailRecipientsLine", originalRecipientsLine);
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailRecipients", originalRecipients);
    }

    @Test
    void noSchedulerEmailConfiguredRendersNothingAndLogsNothing() {
        processor.sendEmail(job(), JobEmailProcessor.emailSubjectError, JobEmailProcessor.emailTemplateError,
                JobEmailProcessor.EMAIL_TEMPLATE_ERROR);

        verifyNoInteractions(registryAccessor, templateEnginesManager);
        assertEquals(List.of(), errors());
    }

    @Test
    void senderWithoutAnyRecipientRendersNothingAndLogsNothing() {
        configure("scheduler@example.com", null);
        when(jobEmailService.findAllByJobName(JOB_NAME)).thenReturn(List.of());

        processor.sendEmail(job(), JobEmailProcessor.emailSubjectNormal, JobEmailProcessor.emailTemplateNormal,
                JobEmailProcessor.EMAIL_TEMPLATE_NORMAL);

        verifyNoInteractions(registryAccessor, templateEnginesManager);
        assertEquals(List.of(), errors());
    }

    @Test
    void dueEmailWithoutTemplateEngineIsStillAnError() {
        configure("scheduler@example.com", null);
        when(jobEmailService.findAllByJobName(JOB_NAME)).thenReturn(
                List.of(new JobEmail("/project/nightly.job", "ops", null, Set.of(), JOB_NAME, "ops@example.com")));
        when(registryAccessor.getRegistryContent(any(), any())).thenReturn("{{job.name}}".getBytes(StandardCharsets.UTF_8));

        processor.sendEmail(job(), JobEmailProcessor.emailSubjectError, JobEmailProcessor.emailTemplateError,
                JobEmailProcessor.EMAIL_TEMPLATE_ERROR);

        assertEquals(List.of(NO_TEMPLATE_ENGINE), errors());
    }

    private static void configure(String sender, String recipientsLine) {
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailSender", sender);
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailRecipientsLine", recipientsLine);
        ReflectionTestUtils.setField(JobEmailProcessor.class, "emailRecipients", recipientsLine == null ? null : recipientsLine.split(","));
    }

    private static Job job() {
        Job job = new Job();
        job.setName(JOB_NAME);
        return job;
    }

    private List<String> errors() {
        return appender.list.stream()
                            .filter(event -> event.getLevel() == Level.ERROR)
                            .map(ILoggingEvent::getFormattedMessage)
                            .toList();
    }
}
