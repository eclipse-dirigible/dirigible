/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.synchronizer;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.parser.ScheduledMetadata;
import org.eclipse.dirigible.components.jobs.parser.ScheduledParser;
import org.eclipse.dirigible.components.jobs.service.JobEmailService;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class JobSynchronizer.
 */
@Component
@Order(SynchronizersOrder.JOB)
public class JobSynchronizer extends MultitenantBaseSynchronizer<Job, Long> {

    /**
     * The Constant FILE_JOB_EXTENSION.
     */
    public static final String FILE_EXTENSION_JOB = ".job";

    public static final String[] FILE_EXTENSIONS_JOB = new String[] {".job", "job.ts", "scheduled.ts"};

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(JobSynchronizer.class);
    /**
     * The job service.
     */
    private final JobService jobService;

    /** The jobs manager. */
    private final JobsManager jobsManager;

    /** The job email service. */
    private final JobEmailService jobEmailService;

    /** The job log service. */
    private final JobLogService jobLogService;

    private final ScheduledParser scheduledParser = new ScheduledParser();

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new job synchronizer.
     *
     * @param jobService the job service
     * @param jobsManager the jobs manager
     * @param jobEmailService the job email service
     * @param jobLogService the job log service
     */
    @Autowired
    JobSynchronizer(JobService jobService, JobsManager jobsManager, JobEmailService jobEmailService, JobLogService jobLogService) {
        this.jobService = jobService;
        this.jobsManager = jobsManager;
        this.jobEmailService = jobEmailService;
        this.jobLogService = jobLogService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Job.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Job> parseImpl(String location, byte[] content) throws ParseException {
        Job job = null;
        String source = new String(content, StandardCharsets.UTF_8);
        if (location.endsWith(FILE_EXTENSION_JOB)) {
            job = JsonHelper.fromJson(source, Job.class);
        } else {
            ScheduledMetadata metadata = scheduledParser.parse(location, source);
            if (metadata.getExpression() == null) {
                return new ArrayList<Job>();
            }
            job = new Job();
            job.setExpression(metadata.getExpression());
            job.setGroup(metadata.getGroup() != null ? metadata.getGroup() : "defined");
            String handler = location;
            if (handler.startsWith(IRepositoryStructure.SEPARATOR)) {
                handler = handler.substring(1);
            }
            job.setHandler(handler);
        }
        Configuration.configureObject(job);
        job.setLocation(location);
        job.setName(FilenameUtils.getBaseName(location));
        job.setType(Job.ARTEFACT_TYPE);
        job.updateKey();
        for (JobParameter j : job.getParameters()) {
            j.setJob(job);
        }
        try {
            Job maybe = getService().findByKey(job.getKey());
            if (maybe != null) {
                job.setId(maybe.getId());
                job.getParameters()
                   .forEach(p -> {
                       JobParameter m = maybe.getParameter(p.getName());
                       if (m != null) {
                           p.setId(m.getId());
                           p.setValue(m.getValue());
                       }
                   });
            }
            Job result = getService().save(job);
            return List.of(result);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("job: {}", job);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Job, Long> getService() {
        return jobService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Job> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Job artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Job> wrapper, ArtefactPhase flow) {
        Job job = wrapper.getArtefact();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(job.getLifecycle())) {
                    try {
                        jobsManager.scheduleJob(job);
                        job.setRunning(true);
                        getService().save(job);
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(job.getLifecycle())) {
                    try {
                        jobsManager.unscheduleJob(job.getName(), job.getGroup());
                        job.setRunning(false);
                        getService().save(job);
                        jobsManager.scheduleJob(job);
                        job.setRunning(true);
                        getService().save(job);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED, e);
                    }
                }
                if (ArtefactLifecycle.FAILED.equals(job.getLifecycle())) {
                    return false;
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(job.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(job.getLifecycle())
                        || ArtefactLifecycle.FAILED.equals(job.getLifecycle())) {
                    try {
                        jobsManager.unscheduleJob(job.getName(), job.getGroup());
                        job.setRunning(false);
                        getService().delete(job);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                break;
            case START:
                if (ArtefactLifecycle.FAILED.equals(job.getLifecycle())) {
                    String message = "Cannot start a Job in a failing state: " + job.getKey();
                    callback.addError(message);
                    callback.registerState(this, wrapper, ArtefactLifecycle.FATAL, message);
                    return true;
                }
                if (job.getRunning() == null || !job.getRunning()) {
                    try {
                        jobsManager.scheduleJob(job);
                        job.setRunning(true);
                        getService().save(job);
                    } catch (Exception e) {
                        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                    }
                }
                break;
            case STOP:
                if (job.getRunning()) {
                    try {
                        jobsManager.unscheduleJob(job.getName(), job.getGroup());
                        job.setRunning(false);
                        getService().save(job);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
                    }
                }
                break;
        }

        return true;
    }

    /**
     * Cleanup.
     *
     * @param job the artefact
     */
    @Override
    public void cleanupImpl(Job job) {
        try {
            jobsManager.unscheduleJob(job.getName(), job.getGroup());
            jobLogService.deleteAllByJobName(job.getName());
            jobEmailService.deleteAllByJobName(job.getName());
            getService().delete(job);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, job, ArtefactLifecycle.DELETED, e);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_JOB;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Job.ARTEFACT_TYPE;
    }

    /**
     * Checks if is accepted.
     *
     * @param file the file
     * @param attrs the attrs
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        for (String extension : FILE_EXTENSIONS_JOB) {
            if (file.toString()
                    .toLowerCase()
                    .endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}
