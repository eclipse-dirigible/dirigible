/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.email.JobEmailProcessor;
import org.eclipse.dirigible.components.jobs.handler.JobHandlerRunner;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
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

    /** Runs a job's handler on the engine it declares - shared with the scheduled fire. */
    private final JobHandlerRunner jobHandlerRunner;

    /**
     * Instantiates a new job service.
     *
     * @param repository the repository
     * @param jobEmailProcessor the job email processor
     * @param jobsManager the jobs manager
     * @param jobHandlerRunner the job handler runner
     */
    public JobService(JobRepository repository, JobEmailProcessor jobEmailProcessor, JobsManager jobsManager,
            JobHandlerRunner jobHandlerRunner) {
        super(repository);
        this.jobEmailProcessor = jobEmailProcessor;
        this.jobsManager = jobsManager;
        this.jobHandlerRunner = jobHandlerRunner;
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
     * Trigger a job now with the given parameters.
     *
     * <p>
     * A job body reads a parameter as a configuration value ({@code job.getParameter(name)} in the
     * JavaScript SDK, {@code Configurations.get(name)} in the Java one), so the parameters have to be
     * visible through {@link Configuration} while the handler runs. Two things bound what that costs
     * (dirigible #6729):
     *
     * <ul>
     * <li><b>Only the job's own declared parameters may be set.</b> The trigger is exposed over REST
     * and to user scripts, so an unbounded key space would let any caller who may trigger a job
     * transiently redefine any configuration key the platform reads. The {@code .job} artefact already
     * declares its parameters, so this is a check against data the platform owns.</li>
     * <li><b>The values are thread-scoped, not global.</b> They go into the thread configuration
     * introduced for tenant overrides (#6205) rather than the process-global RUNTIME layer, so a
     * trigger cannot change what a concurrent request - or another tenant - reads. The previous memento
     * over {@code Configuration.set} also permanently pinned the resolved value of every key it touched
     * into the RUNTIME layer; nothing is written outside this thread now.</li>
     * </ul>
     *
     * @param name the name
     * @param parametersMap the parameters, keyed by the declared parameter name
     * @return true, if successful
     * @throws IllegalArgumentException if the job does not exist, or a parameter it does not declare
     *         was passed
     * @throws Exception the exception
     */
    public boolean trigger(String name, Map<String, String> parametersMap) throws Exception {
        Job job = findByName(name);
        Map<String, String> parameters = (parametersMap != null) ? parametersMap : Collections.emptyMap();
        assertDeclaredParameters(job, parameters);

        Map<String, String> outerConfiguration = Configuration.getThreadConfiguration();
        Map<String, String> jobConfiguration = new HashMap<>(outerConfiguration);
        jobConfiguration.putAll(parameters);
        Configuration.setThreadConfiguration(jobConfiguration);
        try {
            // Run it on the engine the job declares - the same dispatch the scheduled fire uses. A
            // client-Java job's handler is a class name, not a JavaScript path (dirigible #6305).
            jobHandlerRunner.run(job.getHandler(), job.getEngine());
        } finally {
            Configuration.setThreadConfiguration(outerConfiguration);
        }

        return true;
    }

    /**
     * Rejects any parameter the job artefact does not declare.
     *
     * @param job the job being triggered
     * @param parameters the parameters supplied by the caller
     */
    private static void assertDeclaredParameters(Job job, Map<String, String> parameters) {
        List<JobParameter> declaredParameters = job.getParameters();
        Set<String> declared = (declaredParameters != null) ? declaredParameters.stream()
                                                                                .map(JobParameter::getName)
                                                                                .collect(Collectors.toSet())
                : Collections.emptySet();
        List<String> undeclared = parameters.keySet()
                                            .stream()
                                            .filter(key -> !declared.contains(key))
                                            .sorted()
                                            .toList();
        if (!undeclared.isEmpty()) {
            throw new IllegalArgumentException("Job [" + job.getName() + "] does not declare the parameter(s) " + undeclared
                    + ". A trigger may set only the parameters declared by the job artefact: " + declared);
        }
    }

}
