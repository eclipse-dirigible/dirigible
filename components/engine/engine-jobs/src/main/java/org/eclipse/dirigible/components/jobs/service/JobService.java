/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.service;

import static java.text.MessageFormat.format;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.email.JobEmailProcessor;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class JobService.
 */
@Service
@Transactional
public class JobService extends BaseArtefactService<Job, Long> {

    /** The job email processor. */
    private final JobEmailProcessor jobEmailProcessor;

    /** The jobs manager. */
    private final JobsManager jobsManager;

    /**
     * Instantiates a new job service.
     *
     * @param repository the repository
     * @param jobEmailProcessor the job email processor
     * @param jobsManager the jobs manager
     */
    public JobService(JobRepository repository, JobEmailProcessor jobEmailProcessor, JobsManager jobsManager) {
        super(repository);
        this.jobEmailProcessor = jobEmailProcessor;
        this.jobsManager = jobsManager;
    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the job
     */
    @Override
    @Transactional(readOnly = true)
    public Job findByName(String name) {
        String jobName = (name != null && name.startsWith("/")) ? name.substring(1) : name;
        return getRepo().findByName(jobName)
                        .orElseThrow(() -> new IllegalArgumentException("Job with name does not exist: " + jobName));
    }

    /**
     * Save.
     *
     * @param job the job
     * @return the job
     */
    @Override
    public Job save(Job job) {
        Job existing = null;
        try {
            existing = findByName(job.getName());
        } catch (Exception e) {
            // ignore if does not exist yet
        }
        if (existing != null) {
            if (existing.isEnabled() && !job.isEnabled()) {
                String content = jobEmailProcessor.prepareEmail(job, JobEmailProcessor.emailTemplateDisable,
                        JobEmailProcessor.EMAIL_TEMPLATE_DISABLE);
                jobEmailProcessor.sendEmail(job, JobEmailProcessor.emailSubjectDisable, content);
            } else if (!existing.isEnabled() && job.isEnabled()) {
                String content =
                        jobEmailProcessor.prepareEmail(job, JobEmailProcessor.emailTemplateEnable, JobEmailProcessor.EMAIL_TEMPLATE_ENABLE);
                jobEmailProcessor.sendEmail(job, JobEmailProcessor.emailSubjectEnable, content);
            }
        }
        return getRepo().saveAndFlush(job);
    }

    /**
     * Enable.
     *
     * @param name the name
     * @return the job
     * @throws Exception the exception
     */
    public Job enable(String name) throws Exception {
        Job job = findByName(name);
        job.setEnabled(true);
        jobsManager.scheduleJob(job);
        return getRepo().saveAndFlush(job);
    }

    /**
     * Disable.
     *
     * @param name the name
     * @return the job
     * @throws Exception the exception
     */
    public Job disable(String name) throws Exception {
        Job job = findByName(name);
        job.setEnabled(false);
        jobsManager.unscheduleJob(job.getName(), job.getGroup());
        return getRepo().saveAndFlush(job);
    }

    /**
     * Trigger.
     *
     * @param name the name
     * @param parametersMap the parameters map
     * @return true, if successful
     * @throws Exception the exception
     */
    public boolean trigger(String name, Map<String, String> parametersMap) throws Exception {
        Job job = findByName(name);
        if (job == null) {
            String error = format("Job with name {0} does not exist, hence cannot be triggered", name);
            throw new Exception(error);
        }
        Map<String, String> memento = new HashMap<String, String>();
        try {
            for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
                memento.put(entry.getKey(), Configuration.get(entry.getKey()));
                Configuration.set(entry.getKey(), entry.getValue());
            }

            String handler = job.getHandler();
            Path handlerPath = Path.of(handler);

            try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner()) {
                runner.run(handlerPath);
            } catch (Exception e) {
                throw new Exception(e);
            }
        } finally {
            for (Map.Entry<String, String> entry : memento.entrySet()) {
                Configuration.set(entry.getKey(), entry.getValue());
            }
        }

        return true;
    }

}
