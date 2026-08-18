/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Resolves the maven dependencies declared across the registry projects at startup - ordered after
 * the synchronization initializer, so the project.json files are present in the registry. The
 * resolved jars are activated through the modules classloader immediately; the run also arms the
 * declaration watcher by recording the first fingerprint. A frozen instance
 * ({@code DIRIGIBLE_DEPENDENCIES_FROZEN=true}) boots through here too - its activation verifies
 * every locked artifact's checksum before anything serves.
 */
@Order(ApplicationReadyEventListeners.DEPENDENCIES_INITIALIZER)
@Component
class DependenciesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesInitializer.class);

    /** The dependencies service. */
    private final DependenciesService dependenciesService;

    /**
     * Instantiates a new dependencies initializer.
     *
     * @param dependenciesService the dependencies service
     */
    DependenciesInitializer(DependenciesService dependenciesService) {
        this.dependenciesService = dependenciesService;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!dependenciesService.isDynamicEnabled() && !dependenciesService.isFrozen()) {
            LOGGER.debug("Dynamic dependency resolution is disabled");
            return;
        }
        try {
            dependenciesService.resolveAndActivate();
        } catch (RuntimeException e) {
            // per-coordinate failures are reported in the result; whatever still escapes
            // must never prevent the platform from booting
            LOGGER.error("Maven dependency resolution failed", e);
        }
    }

}
