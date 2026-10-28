/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.debug.java.config;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.debug.java.JavaDebugManager;
import org.eclipse.dirigible.components.ide.debug.java.handler.JavaDebugWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the Java Debug WebSocket endpoint at {@code /websockets/ide/java-debug}; its handshake
 * is refused with 503 while the debugger is unavailable.
 */
@Configuration
@EnableWebSocket
public class JavaDebugWebSocketConfig implements WebSocketConfigurer {

    private final JavaDebugWebSocketHandler handler;

    private final JavaDebugManager manager;

    public JavaDebugWebSocketConfig(JavaDebugWebSocketHandler handler, JavaDebugManager manager) {
        this.handler = handler;
        this.manager = manager;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, BaseEndpoint.PREFIX_ENDPOINT_WEBSOCKETS + "ide/java-debug")
                .addInterceptors(new JavaDebugAvailabilityInterceptor(manager));
    }
}
