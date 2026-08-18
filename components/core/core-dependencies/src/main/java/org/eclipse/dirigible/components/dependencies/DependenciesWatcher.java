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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Watches the registry's project.json maven declarations and runs the swap pipeline when they
 * change - so a published dependency change takes effect without a restart and without a manual
 * resolve call. The check is a cheap fingerprint comparison over the collected declarations; the
 * pipeline runs only on an actual change, so a persistently failing declaration is retried only
 * when it changes again.
 */
@Component
class DependenciesWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesWatcher.class);

    /** The dependencies service. */
    private final DependenciesService dependenciesService;

    /** The collector. */
    private final ProjectDependenciesCollector collector;

    /**
     * Instantiates a new dependencies watcher.
     *
     * @param dependenciesService the dependencies service
     * @param collector the collector
     */
    DependenciesWatcher(DependenciesService dependenciesService, ProjectDependenciesCollector collector) {
        this.dependenciesService = dependenciesService;
        this.collector = collector;
    }

    /**
     * One watch tick. Armed only after the boot-time resolution recorded the first fingerprint, so the
     * watcher never races the startup sequence.
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 5_000)
    void watch() {
        if (!dependenciesService.isDynamicEnabled()) {
            return;
        }
        String last = dependenciesService.lastDeclaredFingerprint();
        if (last == null) {
            return;
        }
        String current = collector.collect()
                                  .fingerprint();
        if (current.equals(last)) {
            return;
        }
        LOGGER.info("The registry's maven dependency declarations changed - running the swap pipeline");
        try {
            dependenciesService.resolveAndActivate();
        } catch (RuntimeException e) {
            // the installed generation keeps serving; the next declaration change retries
            LOGGER.error("The dependency swap pipeline failed", e);
        }
    }

}
