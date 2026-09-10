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
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
 * consumer is therefore also a {@link TenantPostProvisioningStep} that tops up what it is tracking
 * once a provisioning round completes, exactly as the sibling {@code ListenerClassConsumer} tops up
 * a late tenant's subscriptions.
 *
 * <p>
 * <b>A pass registers only what is missing.</b> The top-up and the reconciliation retry are both
 * per {@code (job, tenant)}: a registration that landed is recorded and skipped from then on. A
 * registration is idempotent, so repeating it would be harmless in isolation - but a single job the
 * scheduler refuses keeps the retry timer firing, and covering the healthy ones on every tick would
 * mean an N x T {@code saveAndFlush} + Quartz reschedule + INFO line every 30 s for the life of the
 * failure, rewriting the same shared {@code DIRIGIBLE_JOBS} rows from every node of the cluster and
 * burying the one WARN that is the actual fault (#7265).
 */
@Component
@Order(400)
public class ScheduledClassConsumer implements JavaClassConsumer, DisposableBean, TenantPostProvisioningStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledClassConsumer.class);

    /** The user-defined job group (the only group routed through the handler/engine dispatch). */
    private static final String JOB_GROUP = "defined";

    /** Separator of the {@code (job name, tenant id)} bookkeeping key. */
    private static final String KEY_SEPARATOR = "@";

    private final ComponentContainer componentContainer;
    private final JobsManager jobsManager;
    private final JobService jobService;
    private final TenantContext tenantContext;

    /** fqn -> what it declared, plus which of those registrations landed in which tenant. */
    private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<>();

    /** Set while a declared job is not registered in every tenant; drives {@link #reconcile()}. */
    private final AtomicBoolean registrationsIncomplete = new AtomicBoolean();

    /**
     * The {@code (job, tenant)} registrations already reported as failing, so a retry on a timer does
     * not log the same line forever. Keyed per tenant, not per job: a job the scheduler refuses in one
     * tenant only must still get its own first WARN for the next tenant it fails in.
     */
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

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
        // Track what the class DECLARES, not what happened to register: a registration that threw - the
        // scheduler or the tenant's database briefly unavailable - must stay retryable, and comparing the
        // previous names against the successful ones instead would make a transient failure DELETE the
        // Job row a previous run created, along with the operator's enabled flag on it.
        retainOnly(info.fqn(), declarations);
        // A reload installs a FRESH registration, with nothing recorded as landed - so it does re-register
        // every job of the class in every tenant, which is the point: the cron the class declares, or the
        // method a handler stands for, may have changed since the previous generation.
        Registration registration = new Registration(declarations);
        registrations.put(info.fqn(), registration);
        register(info.fqn(), registration);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        unregister(info.fqn());
        LOGGER.info("Unscheduled Java class [{}].", info.fqn());
    }

    /**
     * Top up the registrations the provisioned tenants do not have yet, so a tenant provisioned after
     * the last client-Java rebuild gets its {@code Job} rows and Quartz triggers too. Called once a
     * provisioning round has actually provisioned something - {@code TenantsProvisioner} skips the
     * post-provisioning steps when no tenant was in INITIAL status - so this is not a per-round cost.
     */
    @Override
    public void execute() {
        topUp();
    }

    /**
     * Re-attempt the registrations that did not land, on the reconciliation timer. Gated on the flag,
     * so an instance whose jobs are all registered pays nothing per tick - not even the tenant lookup
     * the fan-out performs - and the flag is cleared before the pass, never after, or a failure the
     * pass itself records would be lost.
     */
    @Override
    public void reconcile() {
        if (!registrationsIncomplete.compareAndSet(true, false)) {
            return;
        }
        topUp();
    }

    /**
     * One pass over everything loaded, registering only the {@code (job, tenant)} pairs still missing.
     * The tenant fan-out happens <b>once for the pass</b> rather than once per class, because
     * {@link TenantContext#executeForEachTenant} reads the provisioned tenants from the database and a
     * per-class fan-out would turn one tick into one query per registered class.
     */
    private void topUp() {
        forEachTenant(tenantId -> registrations.forEach((fqn, registration) -> registerTenant(fqn, registration, tenantId)),
                "top up the client-Java job registrations of the provisioned tenants");
        reportIncomplete();
    }

    /** Register one class's jobs in every provisioned tenant that does not have them yet. */
    private void register(String fqn, Registration registration) {
        forEachTenant(tenantId -> registerTenant(fqn, registration, tenantId),
                "register the client-Java jobs of [" + fqn + "] for the provisioned tenants");
        reportIncomplete();
    }

    /**
     * Run the given work once per provisioned tenant, with that tenant's context active.
     *
     * <p>
     * The per-tenant try is not decoration: {@link TenantContext#executeForEachTenant} propagates the
     * FIRST throw, so without it anything escaping the work in one tenant aborts the fan-out and leaves
     * every tenant behind that one untouched - while the tenants in front of it, already done, would be
     * re-registered by the retry that failure schedules.
     *
     * @param work what to do in the tenant whose id it is given
     * @param description what this fan-out was for, for the log if the fan-out itself fails
     */
    private void forEachTenant(Consumer<String> work, String description) {
        try {
            tenantContext.executeForEachTenant(() -> {
                String tenantId = tenantContext.getCurrentTenant()
                                               .getId();
                try {
                    work.accept(tenantId);
                } catch (RuntimeException e) {
                    registrationsIncomplete.set(true);
                    LOGGER.warn("Failed to register the client-Java jobs of tenant [{}]: {}", tenantId, e.getMessage(), e);
                }
                return null;
            });
        } catch (Exception e) {
            registrationsIncomplete.set(true);
            LOGGER.error("Failed to {}: {}", description, e.getMessage(), e);
        }
    }

    /**
     * Register one class's outstanding jobs in the tenant whose context is currently active.
     *
     * <p>
     * Serialized against the other registrations: the post-provisioning top-up runs on the provisioning
     * thread and a rebuild registers from the synchronizer's, and the find-then-save below is exactly
     * the window in which two of them could insert the same job row twice.
     */
    private synchronized void registerTenant(String fqn, Registration registration, String tenantId) {
        for (JobDeclaration declaration : registration.declarations()) {
            // Re-checked per declaration, not once for the class: an unpublish landing mid-pass drops
            // the entry and deletes the rows, and registering the rest of a class that no longer
            // exists would recreate them.
            if (isSuperseded(fqn, registration)) {
                return;
            }
            if (registration.isRegistered(declaration, tenantId)) {
                continue;
            }
            if (registerJob(declaration, tenantId)) {
                registration.registered(declaration, tenantId);
            }
        }
    }

    /**
     * Register one job (a JobHandler class or a single @Scheduled method) as a Job on the shared
     * scheduler, in the tenant whose context is currently active. Each tenant gets its own scheduled
     * row + tenant-prefixed Quartz job, like the JS {@code .job}/{@code scheduled.ts} synchronizer
     * does: the job body runs in that tenant's context at fire time (the jobs engine restores it from
     * the job data), so a global client bean's repository access is correctly tenant-scoped.
     *
     * @param declaration the job to register
     * @param tenantId the tenant whose context is active, for the log and the bookkeeping key
     * @return whether the registration landed
     */
    private boolean registerJob(JobDeclaration declaration, String tenantId) {
        String name = declaration.name();
        String handler = declaration.handler();
        String expression = declaration.expression();
        String key = key(name, tenantId);
        try {
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
            reportedFailures.remove(key);
            LOGGER.info("Registered client-Java job [{}] (handler [{}]) with cron '{}' for tenant [{}] on the shared scheduler.", name,
                    handler, expression, tenantId);
            return true;
        } catch (Exception e) {
            registrationsIncomplete.set(true);
            String message = "Failed to register client-Java job [" + handler + "] with cron '" + expression + "' for tenant [" + tenantId
                    + "] - it does not fire until it is registered; retrying on the next reconciliation pass.";
            // Once, then quietly: the retry runs on a timer, and a job that can never be registered would
            // otherwise log the same line every tick for the life of the process.
            if (reportedFailures.add(key)) {
                LOGGER.warn("{} {}", message, e.getMessage(), e);
            } else {
                LOGGER.debug("{} {}", message, e.getMessage(), e);
            }
            return false;
        }
    }

    /**
     * Whether this registration has been replaced or unloaded since the caller picked it up. A pass
     * iterates a weakly consistent map, so a republish or an unpublish can drop it in between - and
     * registering it anyway would recreate rows the unload has just deleted.
     */
    private boolean isSuperseded(String fqn, Registration registration) {
        return registrations.get(fqn) != registration;
    }

    /**
     * One line per pass while anything is still down. The per-attempt log falls to DEBUG after the
     * first failure, so this summary is what keeps a lasting outage visible instead of scrolling past
     * once at boot.
     */
    private void reportIncomplete() {
        if (!registrationsIncomplete.get() || reportedFailures.isEmpty()) {
            // An empty set means the fan-out itself failed, which the outer catch has already reported.
            return;
        }
        LOGGER.warn("[{}] client-Java job registration(s) did not land, so those jobs do not fire: {}."
                + " Retrying on the next reconciliation pass.", reportedFailures.size(), new TreeSet<>(reportedFailures));
    }

    @Override
    public void destroy() {
        // The Job rows + Quartz triggers are the persistent, cluster-shared definition - leave them on
        // shutdown (other nodes keep running them; a restart re-registers idempotently). Just drop the
        // local tracking.
        registrations.clear();
        reportedFailures.clear();
    }

    /**
     * Drop the jobs a class registered before but no longer declares - a {@code @Scheduled} method that
     * was renamed or removed. The ones it still declares are re-registered onto their existing rows, so
     * they keep their operator state.
     */
    private void retainOnly(String fqn, List<JobDeclaration> current) {
        Registration previous = registrations.get(fqn);
        if (previous == null) {
            return;
        }
        List<String> currentNames = current.stream()
                                           .map(JobDeclaration::name)
                                           .toList();
        List<String> stale = previous.declarations()
                                     .stream()
                                     .map(JobDeclaration::name)
                                     .filter(name -> !currentNames.contains(name))
                                     .toList();
        remove(stale);
    }

    /** Unschedule + remove the Job rows a class previously registered (per tenant). */
    private void unregister(String fqn) {
        Registration registration = registrations.remove(fqn);
        if (registration == null) {
            return;
        }
        remove(registration.declarations()
                           .stream()
                           .map(JobDeclaration::name)
                           .toList());
    }

    /** Unschedule + delete the given job rows, per tenant. */
    private synchronized void remove(List<String> names) {
        for (String name : names) {
            reportedFailures.removeIf(key -> key.startsWith(name + KEY_SEPARATOR));
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

    private static String key(String name, String tenantId) {
        return name + KEY_SEPARATOR + tenantId;
    }

    /**
     * What one loaded class declares, and where those declarations have already landed. The per-tenant
     * bookkeeping is what keeps a reconciliation pass proportional to what is still missing rather than
     * to everything loaded: a registration is idempotent, but repeating it costs a row write, a Quartz
     * reschedule and an INFO line per job per tenant per tick (#7265).
     */
    private static final class Registration {

        private final List<JobDeclaration> declarations;

        /** {@link ScheduledClassConsumer#key(String, String)} of every registration that landed. */
        private final Set<String> completed = ConcurrentHashMap.newKeySet();

        Registration(List<JobDeclaration> declarations) {
            this.declarations = declarations;
        }

        List<JobDeclaration> declarations() {
            return declarations;
        }

        boolean isRegistered(JobDeclaration declaration, String tenantId) {
            return completed.contains(key(declaration.name(), tenantId));
        }

        void registered(JobDeclaration declaration, String tenantId) {
            completed.add(key(declaration.name(), tenantId));
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
