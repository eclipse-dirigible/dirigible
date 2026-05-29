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

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppParser;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Quartz job that, on every fire, looks up every LOCAL native app whose start mode is ALWAYS and
 * restarts any whose owned process is no longer alive.
 *
 * <p>
 * One global job rather than one-per-app: lighter on Quartz, easier observability. Instantiated by
 * Quartz; fields wired by {@code AutoWiringSpringBeanJobFactory}.
 */
public class NativeAppMonitorJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppMonitorJob.class);

    @Autowired
    private NativeAppRegistry registry;

    @Autowired
    private NativeAppProcessManager processManager;

    @Override
    public void execute(JobExecutionContext context) {
        List<NativeApp> apps = registry.findAll();
        for (NativeApp app : apps) {
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
