/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.graalvm.polyglot.Value;
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
     * @param context the context
     * @return the object
     * @throws Exception the exception
     */
    public Object processEvent(String endpoint, Map<Object, Object> context) throws Exception {
        Websocket websocket = websocketService.findByEndpoint(endpoint);
        String module = websocket.getHandler();
        // String engine = websocket.getEngine();
        try {
            // if (engine == null) {
            // engine = "javascript";
            // }
            context.put("handler", module);

            if ("onmessage".equals(context.get("method"))) {
                return executeOnMessageHandler(module, context);
            } else if ("onopen".equals(context.get("method"))) {
                executeOnOpenHandler(module, context);
            } else if ("onclose".equals(context.get("method"))) {
                executeOnCloseHandler(module, context);
            } else if ("onerror".equals(context.get("method"))) {
                executeOnErrorHandler(module, context);
            }

            return null;

        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private String executeOnMessageHandler(String path, Map<Object, Object> context) {
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner(context)) {
            Module module = runner.run(path);
            Value result = runner.runMethod(module, "onMessage", context.get("message"), context.get("from"));
            return result != null ? result.toString() : "";
        }
    }

    private String executeOnOpenHandler(String path, Map<Object, Object> context) {
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner(context)) {
            Module module = runner.run(path);
            Value result = runner.runMethod(module, "onOpen");
            return result != null ? result.toString() : "";
        }
    }

    private String executeOnCloseHandler(String path, Map<Object, Object> context) {
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner(context)) {
            Module module = runner.run(path);
            Value result = runner.runMethod(module, "onClose");
            return result != null ? result.toString() : "";
        }
    }

    private String executeOnErrorHandler(String path, Map<Object, Object> context) {
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner(context)) {
            Module module = runner.run(path);
            Value result = runner.runMethod(module, "onError", context.get("error"));
            return result != null ? result.toString() : "";
        }
    }

    /**
     * Creates the JS code runner.
     *
     * @return the dirigible javascript code runner
     */
    DirigibleJavascriptCodeRunner createJSCodeRunner(Map<Object, Object> context) {
        return new DirigibleJavascriptCodeRunner(context, false);
    }

}
