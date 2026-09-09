/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * The JVM-local watchdog that asks every {@link JavaClassConsumer} to
 * {@link JavaClassConsumer#reconcile() reconcile} what it could not establish earlier - above all a
 * JMS subscription the embedded broker refused while its {@code vm://localhost} transport was still
 * coming up. Without it such a subscription is never retried and the handler stays silent for the
 * life of the process (issue #7217): a client-Java generation is rebuilt only on publish, and the
 * tenant post-provisioning steps run only when a tenant was actually provisioned, so neither of the
 * two triggers the consumers were written against ever arrives on a steady-state instance.
 *
 * <p>
 * <b>Why a timer of its own</b>, rather than the two schedulers already in the platform:
 * <ul>
 * <li>a Quartz {@code SystemJob} fires <b>once cluster-wide</b> (the shared JDBC job store is
 * clustered), and the state being repaired here is per-JVM - the nodes that did not win the trigger
 * would stay unsubscribed forever;</li>
 * <li>Spring's shared {@code TaskScheduler} is a <b>single thread</b> also carrying the
 * access-constraint refresh and the {@code tsc} liveness monitor, while an external broker
 * addressed through a {@code failover:} URL can block {@code createConnection} indefinitely - one
 * unreachable broker would freeze those too.</li>
 * </ul>
 */
@Component
class JavaConsumersReconciler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaConsumersReconciler.class);

    private static final String THREAD_NAME = "dirigible-java-reconciler";

    private final List<JavaClassConsumer> consumers;

    private ScheduledExecutorService executor;

    @Autowired
    JavaConsumersReconciler(List<JavaClassConsumer> consumers) {
        this.consumers = consumers;
    }

    /**
     * Start the timer once the context is up. Deliberately not in the constructor: the consumers' unit
     * tests build them directly, and a timer ticking underneath them would make their call counts
     * non-deterministic.
     */
    @PostConstruct
    void start() {
        long intervalSeconds = DirigibleConfig.JAVA_RECONCILE_INTERVAL_SECONDS.getIntValue();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::reconcileAll, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        LOGGER.info("Client-Java runtime reconciliation scheduled every [{}] second(s).", intervalSeconds);
    }

    @PreDestroy
    void stop() {
        if (null != executor) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * One pass. Each consumer is isolated the same way {@code JavaLoader} isolates them, and the whole
     * body catches {@link Throwable} on purpose: an exception escaping a task submitted to
     * {@code scheduleWithFixedDelay} cancels that task <b>permanently</b> and without a word, which
     * would silently turn the watchdog off for the rest of the process.
     *
     * <p>
     * It takes no lock of its own. {@code JavaLoader.rebuild} is synchronized on the loader and every
     * consumer guards its own state, so locking here could only deadlock against a concurrent rebuild.
     */
    void reconcileAll() {
        try {
            for (JavaClassConsumer consumer : consumers) {
                try {
                    consumer.reconcile();
                } catch (Exception | LinkageError e) {
                    LOGGER.error("Consumer [{}] threw while reconciling: {}", consumer.getClass()
                                                                                      .getSimpleName(),
                            e.getMessage(), e);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Client-Java runtime reconciliation pass failed: {}", t.getMessage(), t);
        }
    }

}
