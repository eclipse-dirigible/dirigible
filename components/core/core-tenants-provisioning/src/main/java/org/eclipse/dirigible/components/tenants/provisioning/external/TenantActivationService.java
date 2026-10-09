/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Activates an externally provisioned tenant: makes it real for the platform, then initializes it -
 * and only it.
 *
 * <p>
 * The order matters. The tenant is moved to {@code PROVISIONED} first and in the request thread,
 * because the per-tenant fan-out only visits provisioned tenants - an initialization started before
 * the flip would skip the very tenant it is for.
 *
 * <p>
 * The initialization creates every per-tenant artefact in this tenant and touches no other tenant
 * (#7799): the others are initialized already, and running their artefacts again would alter their
 * tables and re-import seed rows they deleted. It takes tens of seconds to minutes, so it runs on
 * an executor and the caller polls. What the caller must not see in between is a completed
 * initialization it never waited for, so the tenant's {@link TenantInitialization} is opened
 * synchronously, before the response - the status reads {@code IN_PROGRESS} from then until this
 * tenant's own initialization ends. Activating a tenant again re-opens it and initializes it again,
 * which is how a failed initialization is repaired.
 *
 * <p>
 * The record is durable, so an initialization a restart interrupted is picked up again when the
 * application is ready.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantActivationService implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantActivationService.class);

    /** The tenant service. */
    private final TenantService tenantService;

    /** The data source registration service. */
    private final TenantDataSourceRegistrationService dataSourceRegistrationService;

    /** The initialization of each activated tenant. */
    private final TenantInitializationService initializationService;

    /** The post provisioning steps. */
    private final Set<TenantPostProvisioningStep> postProvisioningSteps;

    /**
     * One initialization at a time: each holds the synchronization slot, so running several
     * concurrently would only make them wait on each other.
     */
    private final ExecutorService executor;

    /** The tenants whose initialization is queued and has not started yet. */
    private final Set<String> queuedTenants = ConcurrentHashMap.newKeySet();

    /**
     * Instantiates a new tenant activation service.
     *
     * @param tenantService the tenant service
     * @param dataSourceRegistrationService the data source registration service
     * @param initializationService the initialization of each activated tenant
     * @param postProvisioningSteps the post provisioning steps
     */
    @Autowired
    TenantActivationService(TenantService tenantService, TenantDataSourceRegistrationService dataSourceRegistrationService,
            TenantInitializationService initializationService, Set<TenantPostProvisioningStep> postProvisioningSteps) {
        this(tenantService, dataSourceRegistrationService, initializationService, postProvisioningSteps,
                Executors.newSingleThreadExecutor(threadFactory()));
    }

    /**
     * Instantiates a new tenant activation service with the given executor - the seam the tests use to
     * run the initialization inline.
     *
     * @param tenantService the tenant service
     * @param dataSourceRegistrationService the data source registration service
     * @param initializationService the initialization of each activated tenant
     * @param postProvisioningSteps the post provisioning steps
     * @param executor the executor to run initializations on
     */
    TenantActivationService(TenantService tenantService, TenantDataSourceRegistrationService dataSourceRegistrationService,
            TenantInitializationService initializationService, Set<TenantPostProvisioningStep> postProvisioningSteps,
            ExecutorService executor) {
        this.tenantService = tenantService;
        this.dataSourceRegistrationService = dataSourceRegistrationService;
        this.initializationService = initializationService;
        this.postProvisioningSteps = postProvisioningSteps;
        this.executor = executor;
    }

    /**
     * Activates the tenant and starts its initialization.
     *
     * @param tenant the tenant
     */
    void activate(Tenant tenant) {
        if (!dataSourceRegistrationService.isRegistered(tenant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tenant [" + tenant.getId() + "] cannot be activated before its data source ["
                            + dataSourceRegistrationService.tenantDataSourceName(tenant) + "] is registered");
        }

        if (TenantStatus.PROVISIONED != tenant.getStatus()) {
            LOGGER.info("Activating tenant [{}] - moving it from [{}] to [{}].", tenant.getId(), tenant.getStatus(),
                    TenantStatus.PROVISIONED);
            tenant.setStatus(TenantStatus.PROVISIONED);
            // saving also evicts the tenant caches, so the tenant is resolvable at once
            tenantService.save(tenant);
        } else {
            LOGGER.info("Tenant [{}] is already active. Re-initializing it.", tenant.getId());
        }

        initializationService.request(tenant.getId());
        scheduleInitialization(tenant.getId());
    }

    /**
     * Resumes the initializations a restart interrupted. Their records are still open, so their tenants
     * would otherwise read {@code IN_PROGRESS} for good.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (TenantInitialization initialization : initializationService.findOpen()) {
            LOGGER.info("Resuming the interrupted initialization of tenant [{}].", initialization.getTenantId());
            scheduleInitialization(initialization.getTenantId());
        }
    }

    /**
     * Queues the initialization of a tenant, unless it is queued already: a queued initialization has
     * not started yet, so it covers this activation too.
     *
     * @param tenantId the tenant id
     */
    private void scheduleInitialization(String tenantId) {
        if (!queuedTenants.add(tenantId)) {
            LOGGER.info("The initialization of tenant [{}] is already queued.", tenantId);
            return;
        }
        executor.execute(() -> {
            queuedTenants.remove(tenantId);
            initialize(tenantId);
        });
    }

    /**
     * Runs every post provisioning step for the tenant. A step that fails must not take the others down
     * with it; its failure is recorded as the failure of the run, beside the per-artefact outcomes the
     * steps record themselves.
     *
     * @param tenantId the tenant id
     */
    private void initialize(String tenantId) {
        try {
            LOGGER.info("Initializing tenant [{}]...", tenantId);
            initializationService.start(tenantId);
            List<String> failures = new ArrayList<>();
            for (TenantPostProvisioningStep step : postProvisioningSteps) {
                try {
                    step.execute(Set.of(tenantId));
                } catch (RuntimeException ex) {
                    LOGGER.error("Post provisioning step [{}] has failed for tenant [{}].", step, tenantId, ex);
                    failures.add("Post provisioning step [" + step + "] has failed: " + ex.getMessage());
                }
            }
            initializationService.finish(tenantId, failures.isEmpty() ? null : String.join("; ", failures));
            LOGGER.info("Initialization of tenant [{}] has completed.", tenantId);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to record the initialization of tenant [{}].", tenantId, ex);
        }
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        executor.shutdownNow();
    }

    /**
     * Thread factory.
     *
     * @return a factory of named daemon threads, so a running initialization never holds up a shutdown
     */
    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "tenant-initialization");
            thread.setDaemon(true);
            return thread;
        };
    }
}
