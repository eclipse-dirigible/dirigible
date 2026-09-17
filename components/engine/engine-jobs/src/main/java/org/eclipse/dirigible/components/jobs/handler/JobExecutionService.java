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

    /**
     * The triggered id a FINISHED/FAILED entry carries when its run has no TRIGGRED anchor to point at
     * - the log was briefly unwritable when the run started (#7148).
     */
    static final long NO_TRIGGERED_ID = 0L;

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

        Date startedAt = new Date();
        JobLog triggered = registerTriggered(name, handler);
        try {
            // A client-Java job dispatches to the Java engine's executor, a JS one to the code
            // runner - inside the same JobLog wrapping either way, so both are equally
            // visible/monitored in the Jobs perspective.
            jobHandlerRunner.run(handler, engine);

            registeredFinished(name, handler, triggered, startedAt);
        } catch (Exception ex) {
            registeredFailed(name, handler, triggered, startedAt, ex);

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
     * Records the run's outcome as FINISHED.
     *
     * <p>
     * A missing TRIGGRED entry costs the outcome its anchor, not its record: the entry is still
     * written, with {@link #NO_TRIGGERED_ID} in place of the id it cannot point at, so the job's
     * last-run stamp and its notification email still fire (#7148).
     *
     * @param name the name
     * @param module the module
     * @param triggered the TRIGGRED entry this run opened, or null when it could not be written
     * @param startedAt when the run started - the triggered-at of an unanchored outcome
     */
    private void registeredFinished(String name, String module, JobLog triggered, Date startedAt) {
        try {
            jobLogService.jobFinished(name, module, triggeredId(name, triggered), triggeredAt(triggered, startedAt));
        } catch (Exception e) {
            LOGGER.error("Failed to register job [{}] as FINISHED.", name, e);
        }
    }

    /**
     * Records the run's outcome as FAILED, with the cause as its message. Unanchored exactly as
     * {@link #registeredFinished} is - a failed run that leaves no trace is the symptom #7075 set out
     * to remove, and the failure branch is where it hurts most (#7148).
     *
     * @param name the name
     * @param module the module
     * @param triggered the TRIGGRED entry this run opened, or null when it could not be written
     * @param startedAt when the run started - the triggered-at of an unanchored outcome
     * @param ex the ex
     */
    private void registeredFailed(String name, String module, JobLog triggered, Date startedAt, Exception ex) {
        try {
            jobLogService.jobFailed(name, module, triggeredId(name, triggered), triggeredAt(triggered, startedAt), ex.getMessage());
        } catch (Exception se) {
            LOGGER.error("Failed to register job [{}] as FAILED. The job failed with [{}]", name, ex, se);
        }
    }

    /**
     * The id of the TRIGGRED entry the outcome belongs to, or {@link #NO_TRIGGERED_ID} when the run has
     * none.
     *
     * @param name the name
     * @param triggered the TRIGGRED entry, or null
     * @return the triggered id to record
     */
    private long triggeredId(String name, JobLog triggered) {
        if (triggered == null) {
            LOGGER.warn("Job [{}] has no TRIGGRED entry - recording its outcome unanchored.", name);
            return NO_TRIGGERED_ID;
        }
        return triggered.getId();
    }

    /**
     * When the run started, taken off its TRIGGRED entry when it has one and off the fire itself when
     * it does not.
     *
     * @param triggered the TRIGGRED entry, or null
     * @param startedAt when the run started
     * @return the triggered-at to record
     */
    private Date triggeredAt(JobLog triggered, Date startedAt) {
        return triggered == null ? startedAt
                : new Date(triggered.getTriggeredAt()
                                    .getTime());
    }
}
