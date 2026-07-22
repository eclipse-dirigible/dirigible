/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.scheduled;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.handler.JavaJobExecutor;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.job.JobHandler;
import org.eclipse.dirigible.sdk.job.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client-Java jobs on the platform's SHARED Quartz
 * scheduler as first-class {@link Job} definitions - exactly like a JS {@code .job} /
 * {@code scheduled.ts} artefact. Two styles, never mixed on one class:
 * <ul>
 * <li><b>self-describing interface</b> - a {@code @Component} bean implementing {@link JobHandler},
 * which supplies its own {@code cron()} and {@code run()};</li>
 * <li><b>method level</b> - public no-arg methods annotated {@link Scheduled @Scheduled}.</li>
 * </ul>
 * Each becomes a {@code Job} row (engine {@value JavaJobExecutor#ENGINE_JAVA}, handler = the client
 * FQN, optionally {@code #method}) persisted via {@link JobService} and scheduled through
 * {@link JobsManager}. Consequences, matching the JS jobs: the job is <b>visible and monitored in
 * the Jobs perspective</b> (a real row + a job-log entry per run), and it fires <b>once
 * cluster-wide</b> (the shared clustered Quartz JDBC store), not once per JVM as the previous
 * private {@code ThreadPoolTaskScheduler} did. At cron time the jobs engine dispatches back to
 * {@link JavaJobExecutorImpl} through the {@link JavaJobExecutor} SPI to run the client bean.
 *
 * <p>
 * The {@code Job} row is registered under the {@link JavaJobExecutor#RUNTIME_LOCATION_PREFIX}
 * synthetic location so the job synchronizer does not reap it as a registry orphan. Hot-reload
 * re-registers the schedule; a genuinely unloaded class unschedules and removes its rows.
 */
@Component
@Order(400)
public class ScheduledClassConsumer implements JavaClassConsumer, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledClassConsumer.class);

    /** The user-defined job group (the only group routed through the handler/engine dispatch). */
    private static final String JOB_GROUP = "defined";

    private final ComponentContainer componentContainer;
    private final JobsManager jobsManager;
    private final JobService jobService;
    private final TenantContext tenantContext;

    /** fqn -> the base job names it registered (a class may declare several @Scheduled methods). */
    private final ConcurrentMap<String, List<String>> registered = new ConcurrentHashMap<>();

    @Autowired
    public ScheduledClassConsumer(ComponentContainer componentContainer, JobsManager jobsManager, JobService jobService,
            TenantContext tenantContext) {
        this.componentContainer = componentContainer;
        this.jobsManager = jobsManager;
        this.jobService = jobService;
        this.tenantContext = tenantContext;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return JobHandler.class.isAssignableFrom(clazz) || hasScheduledMethod(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("Scheduled job [{}] was not instantiated as a bean - a JobHandler and a @Scheduled method both require "
                    + "the class to be a @Component; skipped.", info.fqn());
            return;
        }

        boolean jobHandler = instance instanceof JobHandler;
        boolean methodLevel = hasScheduledMethod(type);
        if (jobHandler && methodLevel) {
            LOGGER.error("[{}] mixes scheduling styles - it implements JobHandler and also declares @Scheduled methods. "
                    + "Use one style or the other; skipped.", info.fqn());
            return;
        }

        unregister(info.fqn());
        List<String> names = new ArrayList<>();

        if (jobHandler) {
            JobHandler job = (JobHandler) instance;
            register(names, info.fqn(), info.fqn(), job.cron());
        } else {
            for (Method method : type.getDeclaredMethods()) {
                Scheduled annotation = method.getAnnotation(Scheduled.class);
                if (annotation == null) {
                    continue;
                }
                if (!isEligibleMethod(method)) {
                    LOGGER.error("@Scheduled method [{}#{}] must be public and take no parameters; skipped.", info.fqn(), method.getName());
                    continue;
                }
                register(names, info.fqn(), info.fqn() + "#" + method.getName(), annotation.expression());
            }
        }

        if (names.isEmpty()) {
            LOGGER.warn("Scheduled job [{}] produced no schedule.", info.fqn());
            return;
        }
        registered.put(info.fqn(), names);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        unregister(info.fqn());
        LOGGER.info("Unscheduled Java class [{}].", info.fqn());
    }

    @Override
    public void destroy() {
        // The Job rows + Quartz triggers are the persistent, cluster-shared definition - leave them on
        // shutdown (other nodes keep running them; a restart re-registers idempotently). Just drop the
        // local tracking.
        registered.clear();
    }

    /**
     * Register one job (a JobHandler class or a single @Scheduled method) as a Job on the shared
     * scheduler.
     */
    private void register(List<String> sink, String fqn, String handler, String expression) {
        String name = handler.replace('#', '.');
        // Register the job PER TENANT (like the JS .job/scheduled.ts synchronizer does): each tenant
        // gets its own scheduled row + tenant-prefixed Quartz job. The job body runs in that tenant's
        // context at fire time (the jobs engine restores it from the job data), so a global client
        // bean's repository access is correctly tenant-scoped. Class loading happens off any tenant
        // thread, hence the explicit executeForEachTenant.
        try {
            tenantContext.executeForEachTenant(() -> {
                // findByName THROWS when absent (it does not return null) - treat that as "new", and
                // otherwise mutate the existing managed row so save() updates it rather than duplicating.
                Job job;
                try {
                    job = jobService.findByName(name);
                } catch (Exception notFound) {
                    job = new Job();
                }
                job.setName(name);
                job.setGroup(JOB_GROUP);
                job.setClazz("");
                job.setHandler(handler);
                job.setEngine(JavaJobExecutor.ENGINE_JAVA);
                job.setExpression(expression);
                job.setSingleton(false);
                job.setEnabled(true);
                job.setDescription("Client-Java scheduled job [" + handler + "]");
                job.setType(Job.ARTEFACT_TYPE);
                job.setLocation(JavaJobExecutor.RUNTIME_LOCATION_PREFIX + handler);
                job.updateKey();
                jobService.save(job);
                jobsManager.scheduleJob(job);
                return null;
            });
            sink.add(name);
            LOGGER.info("Registered client-Java job [{}] (handler [{}]) with cron '{}' on the shared scheduler.", name, handler,
                    expression);
        } catch (Exception e) {
            LOGGER.error("Failed to register client-Java job [{}] with cron '{}': {}", handler, expression, e.getMessage(), e);
        }
    }

    /** Unschedule + remove the Job rows a class previously registered (per tenant). */
    private void unregister(String fqn) {
        List<String> names = registered.remove(fqn);
        if (names == null) {
            return;
        }
        for (String name : names) {
            try {
                tenantContext.executeForEachTenant(() -> {
                    try {
                        jobsManager.unscheduleJob(name, JOB_GROUP);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to unschedule client-Java job [{}]: {}", name, e.getMessage());
                    }
                    try {
                        jobService.delete(jobService.findByName(name));
                    } catch (Exception e) {
                        // findByName throws when the row is already gone - nothing to remove.
                        LOGGER.debug("No client-Java job row [{}] to remove: {}", name, e.getMessage());
                    }
                    return null;
                });
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister client-Java job [{}]: {}", name, e.getMessage());
            }
        }
    }

    private static boolean hasScheduledMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Scheduled.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0 && !method.isSynthetic();
    }
}
