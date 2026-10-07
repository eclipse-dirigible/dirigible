/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Component
public class SynchronizationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationUtil.class);

    /** How long past the configured retry interval the wait below still gives the retry. */
    private static final int RETRY_MARGIN_SECONDS = 30;

    public static void waitForSynchronizationExecution() {
        SynchronizationProcessor synchronizationProcessor = BeanProvider.getBean(SynchronizationProcessor.class);

        LOGGER.debug("Waiting until the synchronization is not needed...");

        // 120s, not 30: right after a publishAll the synchronizer chews through the freshly copied
        // projects, and on loaded CI runners (PostgreSQL/MSSQL matrices) a full cycle over cloned
        // sample projects exceeds 30s - DependsOnIT timed out here while the sync was simply busy.
        // An at-most bound only affects failing runs; passing runs return as soon as the sync idles.
        await().atMost(120, TimeUnit.SECONDS)
               .pollDelay(1, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .until(() -> {
                   boolean synchNotNeeded = !synchronizationProcessor.isSynchronizationNeeded();
                   boolean synchNotRunning = !synchronizationProcessor.isSynchronizationRunning();
                   LOGGER.debug("Synchronization NOT needed: [{}], synch NOT running [{}]", synchNotNeeded, synchNotRunning);
                   return synchNotNeeded && synchNotRunning;
               });
    }

    /**
     * Wait until the synchronizer stays idle for a full quiet period. A plain idle check is not enough
     * right after a publish: the registry file watcher (a polling watcher on some platforms) delivers
     * trailing change events SECONDS after the copy, scheduling one more cycle - which may re-register
     * data-store entities and rebuild their tables mid-test (the store-api sample verification saw its
     * just-inserted rows vanish exactly this way). Requiring the idle condition to HOLD for ten seconds
     * absorbs those late events before the test proceeds.
     */
    public static void waitForStableSynchronization() {
        SynchronizationProcessor synchronizationProcessor = BeanProvider.getBean(SynchronizationProcessor.class);

        awaitFailedArtefactRetry(synchronizationProcessor);

        LOGGER.debug("Waiting until the synchronization stays idle for a quiet period...");

        await().atMost(180, TimeUnit.SECONDS)
               .during(10, TimeUnit.SECONDS)
               .pollInterval(500, TimeUnit.MILLISECONDS)
               .until(() -> !synchronizationProcessor.isSynchronizationNeeded() && !synchronizationProcessor.isSynchronizationRunning());
    }

    /**
     * Give a FAILED artefact its retry before the journey asserts (issue #7364). A pass reports itself
     * finished while an artefact it could not start is FAILED, so the idle check below is already true
     * in the window between the failure and the retry that heals it - and a journey asserting there
     * reads a 404 for a controller whose compile unit failed in that one pass. One unit of a
     * client-Java batch failing is exactly that shape, and the retry (#7248) installed the route
     * seconds after the test had given up.
     * <p>
     * BOUNDED rather than required: an artefact that keeps failing re-arms the retry on every pass, so
     * waiting for a false would hang on an instance carrying a genuinely broken project. The bound is
     * the configured retry interval plus a margin - one retry's worth - after which the wait gives up
     * and says so, leaving the test to fail on its own assertion with the artefact named in the log.
     *
     * @param synchronizationProcessor the processor to ask
     */
    private static void awaitFailedArtefactRetry(SynchronizationProcessor synchronizationProcessor) {
        if (!synchronizationProcessor.isFailedRetryPending()) {
            return;
        }
        long bound = DirigibleConfig.SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS.getIntValue() + RETRY_MARGIN_SECONDS;
        LOGGER.info("A FAILED artefact is waiting for its retry - waiting up to [{}]s for it before asserting...", bound);
        try {
            await().atMost(bound, TimeUnit.SECONDS)
                   .pollInterval(500, TimeUnit.MILLISECONDS)
                   .until(() -> !synchronizationProcessor.isFailedRetryPending());
        } catch (ConditionTimeoutException ex) {
            LOGGER.warn("An artefact is still FAILED after its retry window - continuing; the test's own assertion decides", ex);
        }
    }
}
