/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.service;

import java.util.Map;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.components.websockets.domain.Websocket;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class WebsocketHandler.
 */
@Component
public class WebsocketProcessor {

    /** The websocket service. */
    private final WebsocketService websocketService;

    /** The javascript service. */
    private final JavascriptService javascriptService;

    /**
     * Instantiates a new websocket handler.
     *
     * @param websocketService the websocket service
     * @param javascriptService the javascript service
     */
    @Autowired
    public WebsocketProcessor(WebsocketService websocketService, JavascriptService javascriptService) {
        this.websocketService = websocketService;
        this.javascriptService = javascriptService;
    }

    /**
     * Gets the websocket service.
     *
     * @return the websocket service
     */
    public WebsocketService getWebsocketService() {
        return websocketService;
    }

    /**
     * Gets the javascript service.
     *
     * @return the javascript service
     */
    public JavascriptService getJavascriptService() {
        return javascriptService;
    }

    /**
     * Process the event.
     *
     * @param endpoint the endpoint
     * @param wrapper the wrapper
     * @param context the context
     * @return the object
     * @throws Exception the exception
     */
    public Object processEvent(String endpoint, String wrapper, Map<Object, Object> context) throws Exception {
        Websocket websocket = websocketService.findByEndpoint(endpoint);
        String module = websocket.getHandler();
        // String engine = websocket.getEngine();
        try {
            // if (engine == null) {
            // engine = "javascript";
            // }
            context.put("handler", module);
            RepositoryPath path = new RepositoryPath(wrapper);
            return getJavascriptService().handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

}
