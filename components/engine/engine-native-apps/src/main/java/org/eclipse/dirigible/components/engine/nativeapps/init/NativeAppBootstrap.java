/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.init;

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.registry.NativeAppRegistry;
import org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On {@link ApplicationReadyEvent}, kicks off the start of every ALWAYS-mode LOCAL native app that
 * survived a JVM restart. Subsequent restarts are owned by
 * {@link org.eclipse.dirigible.components.engine.nativeapps.job.NativeAppMonitorJob
 * NativeAppMonitorJob}.
 */
@Component
public class NativeAppBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppBootstrap.class);

    private final NativeAppRegistry registry;
    private final NativeAppProcessManager processManager;

    public NativeAppBootstrap(NativeAppRegistry registry, NativeAppProcessManager processManager) {
        this.registry = registry;
        this.processManager = processManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startAlwaysApps() {
        for (NativeApp app : registry.findAll()) {
            if (app.getKind() != NativeAppKind.LOCAL || app.getStartMode() != StartMode.ALWAYS) {
                continue;
            }
            try {
                NativeAppParser.rehydrateConfig(app);
                processManager.startAsync(app);
                LOGGER.info("Boot-time startup kicked off for native app [{}].", app.getName());
            } catch (RuntimeException ex) {
                LOGGER.warn("Failed to start native app [{}] at boot: {}", app.getName(), ex.getMessage());
            }
        }
    }
}
