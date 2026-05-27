/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.proxy;

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppKind;
import org.eclipse.dirigible.components.engine.nativeapps.domain.StartMode;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * For LOCAL native apps in {@link StartMode#LAZY} mode, ensures the OS process is alive (starting
 * it on demand and waiting until it accepts connections) before letting the dispatcher forward.
 * REMOTE apps and apps already alive are pass-through. ALWAYS-mode apps that are unexpectedly down
 * are also started here as a safety net between monitor-job firings.
 */
@Component
class LazyStartFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LazyStartFilter.class);

    private final NativeAppProcessManager processManager;

    LazyStartFilter(NativeAppProcessManager processManager) {
        this.processManager = processManager;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        NativeApp app = NativeAppProxyAttributes.requireNativeApp(request);
        if (app.getKind() != NativeAppKind.LOCAL) {
            return next.handle(request);
        }
        if (processManager.isAlive(app)) {
            return next.handle(request);
        }
        try {
            processManager.startAndAwaitReady(app);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to start native app [{}] on demand: {}", app.getName(), ex.getMessage(), ex);
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                                 .body("Native app [" + app.getName() + "] is not available: " + ex.getMessage());
        }
        return next.handle(request);
    }
}
