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
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
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
 * re-registers the schedule <b>onto the existing row</b>, which is what preserves the operator's
 * enable/disable choice: that flag belongs to the Jobs perspective, not to the code, so a
 * registration carries it over instead of switching the job back on - a class load happens at every
 * server start and on every client-Java rebuild. A genuinely unloaded class, or a job that a
 * reloaded class no longer declares, unschedules and removes its rows.
 *
 * <p>
 * <b>Tenants outlive a generation.</b> The per-tenant fan-out below runs at class-load time, and a
 * client-Java generation is JVM-wide and only rebuilt when the Java synchronizer goes dirty on a
 * publish - so a tenant provisioned afterwards would have no {@code Job} row and no Quartz trigger
 * for any client-Java job, with nothing in the Jobs perspective to point at the cause. This
 * consumer is therefore also a {@link TenantPostProvisioningStep} that re-registers what it is
 * tracking once a provisioning round completes, exactly as the sibling
 * {@code ListenerClassConsumer} tops up a late tenant's subscriptions. The top-up needs no
 * per-tenant bookkeeping of its own because a registration is idempotent by construction: it lands
 * on the existing row and {@link JobsManager#scheduleJob} returns early once the job and its
 * trigger are there.
 */
@Component
@Order(400)
public class ScheduledClassConsumer implements JavaClassConsumer, DisposableBean, TenantPostProvisioningStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledClassConsumer.class);

    /** The user-defined job group (the only group routed through the handler/engine dispatch). */
    private static final String JOB_GROUP = "defined";

    private final ComponentContainer componentContainer;
    private final JobsManager jobsManager;
    private final JobService jobService;
    private final TenantContext tenantContext;

    /** fqn -> the jobs it declared (a class may declare several @Scheduled methods). */
    private final ConcurrentMap<String, List<JobDeclaration>> registered = new ConcurrentHashMap<>();

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

        // Register what the class declares NOW, then drop only the names it no longer declares -
        // rather than unregistering everything first. A re-registration must find (and update) the
        // existing Job row, because that row carries operator state - above all the enabled flag,
        // which a delete-then-recreate would silently reset to true (#6626).
        List<JobDeclaration> declarations = new ArrayList<>();

        if (jobHandler) {
            JobHandler job = (JobHandler) instance;
            declarations.add(JobDeclaration.of(info.fqn(), job.cron()));
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
                declarations.add(JobDeclaration.of(info.fqn() + "#" + method.getName(), annotation.expression()));
            }
        }

        if (declarations.isEmpty()) {
            unregister(info.fqn());
            LOGGER.warn("Scheduled job [{}] produced no schedule.", info.fqn());
            return;
        }
        List<JobDeclaration> scheduled = new ArrayList<>();
        for (JobDeclaration declaration : declarations) {
            if (register(declaration)) {
                scheduled.add(declaration);
            }
        }
        retainOnly(info.fqn(), scheduled);
        registered.put(info.fqn(), scheduled);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        unregister(info.fqn());
        LOGGER.info("Unscheduled Java class [{}].", info.fqn());
    }

    /**
     * Re-register every job of every loaded class, so a tenant provisioned after the last client-Java
     * rebuild gets its {@code Job} rows and Quartz triggers too. Called once a provisioning round has
     * actually provisioned something - {@code TenantsProvisioner} skips the post-provisioning steps
     * when no tenant was in INITIAL status - so this is not a per-round cost.
     *
     * <p>
     * Unlike the listener consumer's top-up this does not track which tenants it has already covered,
     * and does not need to: re-registering finds the existing row and updates it in place (keeping the
     * operator's enabled flag), and {@link JobsManager#scheduleJob} returns early when the job and its
     * trigger already exist. A second consumer on a JMS destination competes for messages; a second
     * registration of the same job is a no-op.
     */
    @Override
    public void execute() {
        registered.values()
                  .forEach(declarations -> declarations.forEach(this::register));
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
     *
     * <p>
     * Serialized against the other registrations: the post-provisioning top-up runs on the provisioning
     * thread and a rebuild registers from the synchronizer's, and the find-then-save below is exactly
     * the window in which two of them could insert the same job row twice.
     *
     * @return true if the job is now registered in every tenant
     */
    private synchronized boolean register(JobDeclaration declaration) {
        String name = declaration.name();
        String handler = declaration.handler();
        String expression = declaration.expression();
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
                boolean enabled = true;
                try {
                    job = jobService.findByName(name);
                    // The enabled flag belongs to the OPERATOR, not to the code: it is what the Jobs
                    // perspective's enable/disable writes. Carry it over, so a disabled job stays
                    // disabled across restarts and hot reloads instead of quietly firing again - and so
                    // no spurious "job enabled" notification mail goes out (#6626). A brand-new job
                    // starts enabled, like every other artefact-defined one.
                    enabled = job.isEnabled();
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
                job.setEnabled(enabled);
                job.setDescription("Client-Java scheduled job [" + handler + "]");
                job.setType(Job.ARTEFACT_TYPE);
                job.setLocation(JavaJobExecutor.RUNTIME_LOCATION_PREFIX + handler);
                job.updateKey();
                jobService.save(job);
                jobsManager.scheduleJob(job);
                return null;
            });
            LOGGER.info("Registered client-Java job [{}] (handler [{}]) with cron '{}' on the shared scheduler.", name, handler,
                    expression);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register client-Java job [{}] with cron '{}': {}", handler, expression, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Drop the jobs a class registered before but no longer declares - a {@code @Scheduled} method that
     * was renamed or removed. The ones it still declares were just re-registered onto their existing
     * rows, so they keep their operator state.
     */
    private void retainOnly(String fqn, List<JobDeclaration> current) {
        List<JobDeclaration> previous = registered.get(fqn);
        if (previous == null) {
            return;
        }
        List<String> currentNames = current.stream()
                                           .map(JobDeclaration::name)
                                           .toList();
        List<String> stale = previous.stream()
                                     .map(JobDeclaration::name)
                                     .filter(name -> !currentNames.contains(name))
                                     .toList();
        remove(stale);
    }

    /** Unschedule + remove the Job rows a class previously registered (per tenant). */
    private void unregister(String fqn) {
        List<JobDeclaration> declarations = registered.remove(fqn);
        if (declarations == null) {
            return;
        }
        remove(declarations.stream()
                           .map(JobDeclaration::name)
                           .toList());
    }

    /** Unschedule + delete the given job rows, per tenant. */
    private synchronized void remove(List<String> names) {
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

    /**
     * One job a loaded class declares: the {@code Job} row's name, the handler the jobs engine
     * dispatches to ({@code <fqn>} or {@code <fqn>#<method>}), and its cron expression. Kept per class
     * because a re-registration - for a late tenant, above all - needs the whole declaration, not just
     * the name it produced.
     */
    private record JobDeclaration(String name, String handler, String expression) {

        static JobDeclaration of(String handler, String expression) {
            return new JobDeclaration(handler.replace('#', '.'), handler, expression);
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
