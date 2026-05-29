/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.job;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppParser;
import org.eclipse.dirigible.components.jobs.SystemJob;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * System job that, on every fire, looks up every LOCAL+ALWAYS native app and restarts any whose
 * owned process is no longer alive. Auto-scheduled by {@code JobsInitializer} via the
 * {@code DirigibleJob.createTrigger()} machinery because we extend {@link SystemJob} and are a
 * Spring {@code @Component} — no bespoke scheduler bean required.
 *
 * <p>
 * One global job rather than one-per-app: lighter on Quartz, easier observability. Instantiated by
 * Quartz; collaborators wired by {@code AutoWiringSpringBeanJobFactory} — same documented exception
 * to the constructor-injection convention that applies to every {@link SystemJob} subclass in this
 * repo.
 */
@Component
class NativeAppMonitorJob extends SystemJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppMonitorJob.class);

    @Autowired
    private NativeAppRegistry registry;

    @Autowired
    private NativeAppProcessManager processManager;

    @Override
    protected String getJobKey() {
        return this.getClass()
                   .getSimpleName();
    }

    @Override
    protected String getTriggerKey() {
        return this.getClass()
                   .getSimpleName()
                + "Trigger";
    }

    @Override
    protected SimpleScheduleBuilder getSchedule() {
        int seconds = DirigibleConfig.NATIVE_APP_MONITOR_INTERVAL_SECONDS.getIntValue();
        return simpleSchedule().withIntervalInSeconds(seconds)
                               .repeatForever()
                               .withMisfireHandlingInstructionNextWithExistingCount();
    }

    @Override
    public void execute(JobExecutionContext context) {
        for (NativeApp app : registry.findAll()) {
            if (app.getKind() != NativeAppKind.LOCAL || app.getStartMode() != StartMode.ALWAYS) {
                continue;
            }
            try {
                NativeAppParser.rehydrateConfig(app);
                if (!processManager.isAlive(app)) {
                    LOGGER.info("ALWAYS-mode native app [{}] is not running; restarting.", app.getName());
                    processManager.startAsync(app);
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Native-app monitor cycle skipped app [{}]: {}", app.getName(), ex.getMessage(), ex);
            }
        }
    }
}
