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

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Activates an externally provisioned tenant: makes it real for the platform, then materializes its
 * artefacts.
 *
 * <p>
 * The order matters. The tenant is moved to {@code PROVISIONED} first and in the request thread,
 * because the per-tenant fan-out only visits provisioned tenants - a materialization started before
 * the flip would skip the very tenant it is for.
 *
 * <p>
 * The materialization itself is a full synchronization pass, tens of seconds to minutes, so it runs
 * on an executor and the caller polls. What the caller must not see in between is a completed
 * initialization it never waited for, and it would: the derived status reads "everything is
 * processed" until something says otherwise. So the marking - blanking the checksums of the
 * per-tenant definitions - happens synchronously, before the response, and the executor only does
 * the work. The post-provisioning step blanks them again, which is harmless, and that is also why
 * repeating an activation is safe.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantActivationService implements DisposableBean {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantActivationService.class);

    /** The tenant service. */
    private final TenantService tenantService;

    /** The data source registration service. */
    private final TenantDataSourceRegistrationService dataSourceRegistrationService;

    /** The definition service. */
    private final DefinitionService definitionService;

    /** The multitenant synchronizers. */
    private final MultitenantSynchronizers multitenantSynchronizers;

    /** The post provisioning steps. */
    private final Set<TenantPostProvisioningStep> postProvisioningSteps;

    /**
     * One pass at a time, and at most one more queued behind it: a synchronization is global, so
     * running several concurrently would only make them wait on each other, and queueing more than one
     * would repeat work that the queued pass already covers.
     */
    private final ExecutorService executor;

    /** Whether a pass is already queued and has not started yet. */
    private final AtomicBoolean passQueued = new AtomicBoolean(false);

    /**
     * Instantiates a new tenant activation service.
     *
     * @param tenantService the tenant service
     * @param dataSourceRegistrationService the data source registration service
     * @param definitionService the definition service
     * @param multitenantSynchronizers the multitenant synchronizers
     * @param postProvisioningSteps the post provisioning steps
     */
    @Autowired
    TenantActivationService(TenantService tenantService, TenantDataSourceRegistrationService dataSourceRegistrationService,
            DefinitionService definitionService, MultitenantSynchronizers multitenantSynchronizers,
            Set<TenantPostProvisioningStep> postProvisioningSteps) {
        this(tenantService, dataSourceRegistrationService, definitionService, multitenantSynchronizers, postProvisioningSteps,
                Executors.newSingleThreadExecutor(threadFactory()));
    }

    /**
     * Instantiates a new tenant activation service with the given executor - the seam the tests use to
     * run the initialization inline.
     *
     * @param tenantService the tenant service
     * @param dataSourceRegistrationService the data source registration service
     * @param definitionService the definition service
     * @param multitenantSynchronizers the multitenant synchronizers
     * @param postProvisioningSteps the post provisioning steps
     * @param executor the executor to run initializations on
     */
    TenantActivationService(TenantService tenantService, TenantDataSourceRegistrationService dataSourceRegistrationService,
            DefinitionService definitionService, MultitenantSynchronizers multitenantSynchronizers,
            Set<TenantPostProvisioningStep> postProvisioningSteps, ExecutorService executor) {
        this.tenantService = tenantService;
        this.dataSourceRegistrationService = dataSourceRegistrationService;
        this.definitionService = definitionService;
        this.multitenantSynchronizers = multitenantSynchronizers;
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

        markArtefactsForReprocessing();
        scheduleInitialization(tenant.getId());
    }

    /**
     * Blanks the checksums of every per-tenant definition, so the next synchronization reprocesses them
     * and the derived status reports the initialization as running from the moment this call returns.
     */
    private void markArtefactsForReprocessing() {
        Set<String> artefactTypes = multitenantSynchronizers.getArtefactTypes();
        LOGGER.debug("Marking definitions of types [{}] for reprocessing.", artefactTypes);
        definitionService.updateChecksums(StringUtils.EMPTY, artefactTypes);
    }

    /**
     * Queues the initialization, unless one is already queued: a queued pass has not started yet, so it
     * will cover this tenant too.
     *
     * @param tenantId the tenant whose activation asked for it
     */
    private void scheduleInitialization(String tenantId) {
        if (!passQueued.compareAndSet(false, true)) {
            LOGGER.info("An initialization is already queued; it will cover tenant [{}] as well.", tenantId);
            return;
        }
        executor.execute(() -> {
            passQueued.set(false);
            runPostProvisioningSteps(tenantId);
        });
    }

    /**
     * Runs every post provisioning step. A step that fails must not take the others down with it - the
     * failure is visible to the caller through the artefacts it left behind.
     *
     * @param tenantId the tenant whose activation asked for it
     */
    private void runPostProvisioningSteps(String tenantId) {
        LOGGER.info("Initializing tenants, triggered by the activation of tenant [{}]...", tenantId);
        for (TenantPostProvisioningStep step : postProvisioningSteps) {
            try {
                step.execute();
            } catch (RuntimeException ex) {
                LOGGER.error("Post provisioning step [{}] has failed.", step, ex);
            }
        }
        LOGGER.info("Initialization triggered by the activation of tenant [{}] has completed.", tenantId);
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
     * @return a factory of named daemon threads, so a running pass never holds up a shutdown
     */
    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "tenant-initialization");
            thread.setDaemon(true);
            return thread;
        };
    }
}
