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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Watches the registry's project.json maven declarations and runs the swap pipeline when they
 * change - so a published dependency change takes effect without a restart and without a manual
 * resolve call. The check is a cheap stamp comparison over the declaring resources, escalating to a
 * fingerprint over the parsed declarations only when the stamp moved; the pipeline runs only on an
 * actual change, and a pass that failed is retried on the service's own backoff.
 *
 * <p>
 * The tick runs on a dedicated single-thread executor rather than Spring's shared
 * {@code TaskScheduler}: one tick performs a maven resolution (network, subject to connect and read
 * timeouts) and a full javac rebuild of the client codebase, and the shared scheduler's pool holds
 * exactly one thread - which the security access-constraint refresh and the transpiler watchdog
 * also run on. Blocking it for the duration of a resolve-and-rebuild would stop enforcing newly
 * published {@code *.access} constraints.
 */
@Component
class DependenciesWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesWatcher.class);

    /** The delay before the first tick - the boot-time resolution arms the watcher. */
    private static final long INITIAL_DELAY_MILLIS = 10_000;

    /** The delay between ticks. */
    private static final long INTERVAL_MILLIS = 5_000;

    /**
     * Every so many ticks the declarations are collected even when the cheap stamp did not move - the
     * stamp cannot distinguish two writes of equal size within one filesystem timestamp granularity, so
     * a missed edit self-heals within a minute instead of waiting for the next change.
     */
    private static final int FULL_COLLECT_EVERY = 12;

    /** The dependencies service. */
    private final DependenciesService dependenciesService;

    /** The collector. */
    private final ProjectDependenciesCollector collector;

    /** The watcher's own executor - never Spring's shared scheduler, see the class javadoc. */
    private ScheduledExecutorService executor;

    /** The tick counter behind {@link #FULL_COLLECT_EVERY}. */
    private int ticks;

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
     * Starts the watch loop on the dedicated executor.
     */
    @PostConstruct
    void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dirigible-dependencies-watcher");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::tick, INITIAL_DELAY_MILLIS, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the watch loop.
     */
    @PreDestroy
    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * One watch tick, guarded so a failure never kills the scheduled loop.
     */
    private void tick() {
        try {
            watch();
        } catch (RuntimeException | Error e) {
            LOGGER.error("The dependency watch tick failed", e);
        }
    }

    /**
     * One watch tick. Armed only after the boot-time resolution recorded the first fingerprint, so the
     * watcher never races the startup sequence.
     */
    void watch() {
        if (!dependenciesService.isDynamicEnabled() && !dependenciesService.isFrozen()) {
            return;
        }
        String last = dependenciesService.lastDeclaredFingerprint();
        if (last == null) {
            return;
        }
        boolean retryDue = dependenciesService.isRetryDue();
        boolean stampMoved = collector.declarationsMayHaveChanged();
        boolean forceCollect = ++ticks % FULL_COLLECT_EVERY == 0;
        if (!retryDue && !stampMoved && !forceCollect) {
            // nothing under the registry's project.json files moved - no read, no parse, no log
            return;
        }
        if (!retryDue && collector.collect()
                                  .fingerprint()
                                  .equals(last)) {
            return;
        }
        LOGGER.info(retryDue ? "Retrying the dependency swap pipeline after a failed pass"
                : "The registry's maven dependency declarations changed - running the swap pipeline");
        try {
            dependenciesService.resolveAndActivate();
        } catch (RuntimeException e) {
            // the installed generation keeps serving; the next declaration change retries
            LOGGER.error("The dependency swap pipeline failed", e);
        }
    }

}
