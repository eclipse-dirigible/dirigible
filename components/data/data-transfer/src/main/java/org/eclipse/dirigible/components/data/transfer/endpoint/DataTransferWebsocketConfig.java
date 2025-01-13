/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.endpoint;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.transfer.service.DataTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * The Class DataTransferWebsocketConfig.
 */
@Configuration
@EnableWebSocket
public class DataTransferWebsocketConfig implements WebSocketConfigurer {

    /** The data transfer service. */
    private final DataTransferService dataTransferService;

    /**
     * Instantiates a new data transfer websocket config.
     *
     * @param dataTransferService the data transfer service
     */
    @Autowired
    public DataTransferWebsocketConfig(DataTransferService dataTransferService) {
        this.dataTransferService = dataTransferService;
    }

    /**
     * Register web socket handlers.
     *
     * @param registry the registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getDataTransferWebsocketHandler(), BaseEndpoint.PREFIX_ENDPOINT_WEBSOCKETS + "/data/transfer");
    }

    /**
     * Gets the data transfer websocket handler.
     *
     * @return the data transfer websocket handler
     */
    @Bean
    public WebSocketHandler getDataTransferWebsocketHandler() {
        return new DataTransferWebsocketHandler(dataTransferService);
    }

}
