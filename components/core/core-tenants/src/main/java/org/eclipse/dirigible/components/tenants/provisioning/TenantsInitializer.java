/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provisions the tenants a while after the application is ready.
 *
 * <p>
 * The delayed provisioning belongs to this bean's application context: the executor is shut down
 * with the context, so a provisioning that has not started yet never runs. Left to fire after the
 * context closed, it reached through the closed context's beans, which Spring re-creates on demand
 * - an entity manager factory and with it the SystemDB Liquibase update - against a database that
 * meanwhile belongs to another context, where the stray update could leave the changelog lock held
 * and block every later boot on it.
 */
@Order(ApplicationReadyEventListeners.TENANTS_INITIALIZER)
@Component
class TenantsInitializer implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsInitializer.class);

    private static final Duration PROVISIONING_DELAY = Duration.ofSeconds(30);

    private final TenantsProvisioner tenantsProvisioner;

    private final Duration provisioningDelay;

    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    @Autowired
    TenantsInitializer(TenantsProvisioner tenantsProvisioner) {
        this(tenantsProvisioner, PROVISIONING_DELAY);
    }

    TenantsInitializer(TenantsProvisioner tenantsProvisioner, Duration provisioningDelay) {
        this.tenantsProvisioner = tenantsProvisioner;
        this.provisioningDelay = provisioningDelay;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        executor.schedule(this::provisionTenants, provisioningDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void provisionTenants() {
        LOGGER.debug("Initializing tenants...");

        tenantsProvisioner.provision();

        LOGGER.debug("Tenants have been initialized.");
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }

}
