/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import io.opentelemetry.api.trace.Span;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JobExecutionService {

    public static final String JOB_PARAMETER_HANDLER = "dirigible-job-handler";
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);
    public static String JOB_PARAMETER_ENGINE = "dirigible-engine-type";

    private final JobLogService jobLogService;
    private final JobNameCreator jobNameCreator;
    private final DataSourcesManager dataSourcesManager;
    private final JobHandlerRunner jobHandlerRunner;

    JobExecutionService(JobLogService jobLogService, JobNameCreator jobNameCreator, DataSourcesManager dataSourcesManager,
            JobHandlerRunner jobHandlerRunner) {
        this.jobLogService = jobLogService;
        this.jobNameCreator = jobNameCreator;
        this.dataSourcesManager = dataSourcesManager;
        this.jobHandlerRunner = jobHandlerRunner;
    }

    /**
     * Runs the job a Quartz trigger just fired, reading its handler and engine off the fire context.
     *
     * @param context the Quartz execution context
     * @throws JobExecutionException when the job body throws
     */
    public void executeJob(JobExecutionContext context) throws JobExecutionException {
        String tenantJobName = context.getJobDetail()
                                      .getKey()
                                      .getName();
        String name = jobNameCreator.fromTenantName(tenantJobName);

        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        String handler = params.getString(JOB_PARAMETER_HANDLER);
        String engine = params.getString(JOB_PARAMETER_ENGINE);

        context.put("handler", handler);
        executeJob(name, handler, engine);
    }

    /**
     * Runs a job's handler and records the run in the job's execution log.
     *
     * <p>
     * This is the one execution path: the scheduled fire reaches it through the Quartz context above, a
     * manual "Run now" through {@code JobService.trigger}. Both leave the same trail - a TRIGGRED
     * entry, then a FINISHED or FAILED one, and the owning job's last-run status, message and timestamp
     * - because an operator cannot tell a manual run from a scheduled one after the fact, and a run
     * that leaves no trace is indistinguishable from one that never happened (#7075).
     *
     * @param name the job's name, as the artefact declares it
     * @param handler the job's handler - a client-Java FQN for the Java engine, a repository path
     *        otherwise
     * @param engine the job's engine, or null for the default JavaScript one
     * @throws JobExecutionException when the job body throws
     */
    public void executeJob(String name, String handler, String engine) throws JobExecutionException {
        boolean java = JavaJobExecutor.ENGINE_JAVA.equals(engine);
        Span.current()
            .setAttribute("handler", handler);

        JobLog triggered = registerTriggered(name, handler);
        try {
            // A client-Java job dispatches to the Java engine's executor, a JS one to the code
            // runner - inside the same JobLog wrapping either way, so both are equally
            // visible/monitored in the Jobs perspective.
            jobHandlerRunner.run(handler, engine);

            registeredFinished(name, handler, triggered);
        } catch (Exception ex) {
            registeredFailed(name, handler, triggered, ex);

            String msg = "Failed to execute " + (java ? "Java" : "JS") + " job. Job name [" + name + "], handler [" + handler + "]";
            throw new JobExecutionException(msg, ex);
        }
    }

    /**
     * Register triggered.
     *
     * @param name the name
     * @param module the module
     * @return the job log definition
     */
    private JobLog registerTriggered(String name, String module) {
        JobLog triggered = null;
        try {
            triggered = jobLogService.jobTriggered(name, module);
        } catch (Exception e) {
            LOGGER.error("Failed to register job [{}] as TRIGGERED.", name, e);
        }
        return triggered;
    }

    /**
     * Registered finished.
     *
     * @param name the name
     * @param module the module
     * @param triggered the TRIGGRED entry this run opened, or null when it could not be written
     */
    private void registeredFinished(String name, String module, JobLog triggered) {
        if (triggered == null) {
            return;
        }
        try {
            jobLogService.jobFinished(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                         .getTime()));
        } catch (Exception e) {
            LOGGER.error("Failed to register job [{}] as FINISHED.", name, e);
        }
    }

    /**
     * Registered failed.
     *
     * @param name the name
     * @param module the module
     * @param triggered the TRIGGRED entry this run opened, or null when it could not be written
     * @param ex the ex
     */
    private void registeredFailed(String name, String module, JobLog triggered, Exception ex) {
        if (triggered == null) {
            return;
        }
        try {
            jobLogService.jobFailed(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                       .getTime()),
                    ex.getMessage());
        } catch (Exception se) {
            LOGGER.error("Failed to register job [{}] as FAILED. The job failed with [{}]", name, ex, se);
        }
    }
}
