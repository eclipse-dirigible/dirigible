/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.console.service;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * The Class ConsoleWebsocketConfig.
 */
@Configuration
@EnableWebSocket
public class ConsoleWebsocketConfig implements WebSocketConfigurer {

    /**
     * Register web socket handlers.
     *
     * @param registry the registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getConsoleWebsocketHandler(), BaseEndpoint.PREFIX_ENDPOINT_WEBSOCKETS + "ide/console");
    }

    /**
     * Gets the data transfer websocket handler.
     *
     * @return the data transfer websocket handler
     */
    public WebSocketHandler getConsoleWebsocketHandler() {
        return new ConsoleWebsocketHandler();
    }

}
