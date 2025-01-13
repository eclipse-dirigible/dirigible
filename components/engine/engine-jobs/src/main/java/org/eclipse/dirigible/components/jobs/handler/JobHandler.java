/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import io.opentelemetry.api.trace.Span;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Date;

/**
 * The built-in scripting service job handler.
 */
public class JobHandler implements Job {

    /** The Constant TENANT_PARAMETER. */
    public static final String TENANT_PARAMETER = "tenant-id";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobHandler.class);

    /** The handler parameter. */
    public static String JOB_PARAMETER_HANDLER = "dirigible-job-handler";
    /** The engine type. */
    public static String JOB_PARAMETER_ENGINE = "dirigible-engine-type";

    /** The job log service. */
    @Autowired
    private JobLogService jobLogService;

    /** The tenant context. */
    @Autowired
    private TenantContext tenantContext;

    /** The job name creator. */
    @Autowired
    private JobNameCreator jobNameCreator;

    /**
     * Execute.
     *
     * @param context the context
     * @throws JobExecutionException the job execution exception
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        Tenant tenant = (Tenant) params.get(TENANT_PARAMETER);
        if (null == tenant) {
            throw new MissingTenantException(
                    "Missing tenant parameter with key [" + TENANT_PARAMETER + "] for job with details: " + context.getJobDetail());
        }

        try {
            tenantContext.execute(tenant, () -> {
                executeJob(context);
                return null;
            });

        } catch (RuntimeException ex) {
            throw new JobExecutionException("Failed to execute job with details " + context.getJobDetail(), ex);
        }
    }

    /**
     * Execute job.
     *
     * @param context the context
     * @throws JobExecutionException the job execution exception
     */
    private void executeJob(JobExecutionContext context) throws JobExecutionException {
        String tenantJobName = context.getJobDetail()
                                      .getKey()
                                      .getName();
        String name = jobNameCreator.fromTenantName(tenantJobName);

        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        String handler = params.getString(JOB_PARAMETER_HANDLER);
        Span.current()
            .setAttribute("handler", handler);

        JobLog triggered = registerTriggered(name, handler);
        if (triggered != null) {
            context.put("handler", handler);
            Path handlerPath = Path.of(handler);

            try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner()) {
                runner.run(handlerPath);
                registeredFinished(name, handler, triggered);
            } catch (RuntimeException ex) {
                registeredFailed(name, handler, triggered, ex);
                String msg = "Failed to execute JS. Job name [" + name + "], handler [" + handler + "]";
                LOGGER.error(msg, ex);
                throw new JobExecutionException(msg, ex);
            }
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
            LOGGER.error(e.getMessage(), e);
        }
        return triggered;
    }

    /**
     * Registered finished.
     *
     * @param name the name
     * @param module the module
     * @param triggered the triggered
     */
    private void registeredFinished(String name, String module, JobLog triggered) {
        try {
            jobLogService.jobFinished(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                         .getTime()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Registered failed.
     *
     * @param name the name
     * @param module the module
     * @param triggered the triggered
     * @param e the e
     */
    private void registeredFailed(String name, String module, JobLog triggered, Exception e) {
        try {
            jobLogService.jobFailed(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                       .getTime()),
                    e.getMessage());
        } catch (Exception se) {
            LOGGER.error(se.getMessage(), se);
        }
    }

}
