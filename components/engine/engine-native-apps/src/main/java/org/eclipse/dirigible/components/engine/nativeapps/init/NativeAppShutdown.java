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

import jakarta.annotation.PreDestroy;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class NativeAppShutdown {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppShutdown.class);

    private final NativeAppProcessManager processManager;

    NativeAppShutdown(NativeAppProcessManager processManager) {
        this.processManager = processManager;
    }

    @PreDestroy
    void stopAll() {
        LOGGER.info("Stopping all native-app processes during shutdown.");
        processManager.stopAll();
    }
}
