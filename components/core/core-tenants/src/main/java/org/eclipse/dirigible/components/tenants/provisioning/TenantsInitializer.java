/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Order(ApplicationReadyEventListeners.TENANTS_INITIALIZER)
@Component
class TenantsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsInitializer.class);

    private final TenantsProvisioner tenantsProvisioner;

    TenantsInitializer(TenantsProvisioner tenantsProvisioner) {
        this.tenantsProvisioner = tenantsProvisioner;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(this::provisionTenants, 30, TimeUnit.SECONDS);
    }

    private void provisionTenants() {
        LOGGER.debug("Initializing tenants...");

        tenantsProvisioner.provision();

        LOGGER.debug("Tenants have been initialized.");
    }

}
