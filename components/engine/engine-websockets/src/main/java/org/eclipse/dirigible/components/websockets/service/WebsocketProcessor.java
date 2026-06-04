/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.service;

import java.lang.reflect.Method;
import java.util.Map;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.components.websockets.domain.Websocket;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * The Class WebsocketHandler.
 */
@Component
public class WebsocketProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketProcessor.class);

    private static final String JAVA_REGISTRY_CLASS = "org.eclipse.dirigible.engine.java.websocket.JavaWebsocketRegistry";

    /** The websocket service. */
    private final WebsocketService websocketService;

    /** The javascript service. */
    private final JavascriptService javascriptService;

    private final ApplicationContext applicationContext;

    /**
     * Lazily resolved reference to the optional {@code JavaWebsocketRegistry} bean from
     * {@code engine-java}. Resolved once in {@link #initJavaRegistry()} — {@code null} when
     * {@code engine-java} is not on the classpath (e.g. in unit tests for this module).
     */
    private Object javaWebsocketRegistry;

    /** {@code contains(String)} method on the registry, cached to avoid repeated reflection. */
    private Method registryContainsMethod;

    /** {@code get(String)} method on the registry, cached to avoid repeated reflection. */
    private Method registryGetMethod;

    /**
     * Instantiates a new websocket handler.
     *
     * @param websocketService the websocket service
     * @param javascriptService the javascript service
     * @param applicationContext the Spring application context
     */
    @Autowired
    public WebsocketProcessor(WebsocketService websocketService, JavascriptService javascriptService,
            ApplicationContext applicationContext) {
        this.websocketService = websocketService;
        this.javascriptService = javascriptService;
        this.applicationContext = applicationContext;
    }

    /**
     * Attempt to locate the optional {@code JavaWebsocketRegistry} bean. Called after all beans are
     * constructed so that {@code engine-java} has time to register its beans regardless of
     * initialisation order.
     */
    @PostConstruct
    void initJavaRegistry() {
        try {
            Class<?> registryClass = Class.forName(JAVA_REGISTRY_CLASS);
            javaWebsocketRegistry = applicationContext.getBean(registryClass);
            registryContainsMethod = registryClass.getMethod("contains", String.class);
            registryGetMethod = registryClass.getMethod("get", String.class);
        } catch (ReflectiveOperationException e) {
            // engine-java not on classpath — Java websocket support disabled.
        } catch (org.springframework.beans.BeansException e) {
            // bean not registered — Java websocket support disabled.
        }
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
        if (hasJavaHandler(endpoint)) {
            return dispatchToJava(endpoint, context);
        }

        Websocket websocket = websocketService.findByEndpoint(endpoint);
        String module = websocket.getHandler();
        try {
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

    /**
     * Dispatch a WebSocket event to a Java handler retrieved from the optional
     * {@code JavaWebsocketRegistry}. Missing handler methods are silently skipped.
     */
    private Object dispatchToJava(String endpoint, Map<Object, Object> context) {
        Object handler = getJavaHandler(endpoint);
        if (handler == null) {
            return null;
        }
        String method = (String) context.get("method");
        try {
            switch (method == null ? "" : method) {
                case "onmessage" -> {
                    Method m = findMethod(handler.getClass(), "onMessage", String.class, String.class);
                    if (m != null) {
                        Object result = m.invoke(handler, context.get("message"), context.get("from"));
                        return result != null ? result.toString() : "";
                    }
                }
                case "onopen" -> {
                    Method m = findMethod(handler.getClass(), "onOpen");
                    if (m != null) {
                        m.invoke(handler);
                    }
                }
                case "onclose" -> {
                    Method m = findMethod(handler.getClass(), "onClose");
                    if (m != null) {
                        m.invoke(handler);
                    }
                }
                case "onerror" -> {
                    Method m = findMethod(handler.getClass(), "onError", String.class);
                    if (m != null) {
                        m.invoke(handler, context.get("error"));
                    }
                }
                default -> LOGGER.warn("Unknown websocket method [{}] for endpoint [{}]", method, endpoint);
            }
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOGGER.error("Java @Websocket handler [{}] threw on [{}]: {}", endpoint, method, cause.getMessage(), cause);
        }
        return null;
    }

    private boolean hasJavaHandler(String endpoint) {
        if (javaWebsocketRegistry == null || registryContainsMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(registryContainsMethod.invoke(javaWebsocketRegistry, endpoint));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private Object getJavaHandler(String endpoint) {
        if (javaWebsocketRegistry == null || registryGetMethod == null) {
            return null;
        }
        try {
            return registryGetMethod.invoke(javaWebsocketRegistry, endpoint);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
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
