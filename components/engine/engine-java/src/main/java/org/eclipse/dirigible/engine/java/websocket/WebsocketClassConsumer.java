/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.websocket;

import java.lang.reflect.Method;

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.net.OnClose;
import org.eclipse.dirigible.sdk.net.OnError;
import org.eclipse.dirigible.sdk.net.OnMessage;
import org.eclipse.dirigible.sdk.net.OnOpen;
import org.eclipse.dirigible.sdk.net.Websocket;
import org.eclipse.dirigible.sdk.net.WebsocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that binds client WebSocket handlers to an endpoint in the
 * {@link JavaWebsocketRegistry}. Two styles, never mixed on one class:
 * <ul>
 * <li><b>interface</b> — a {@code @Component} bean implementing {@link WebsocketHandler}, which
 * supplies its own {@code endpoint()};</li>
 * <li><b>annotation</b> — a {@link Websocket @Websocket} class with
 * {@code @OnOpen}/{@code @OnMessage}/ {@code @OnError}/{@code @OnClose} methods.</li>
 * </ul>
 * The instance is built (with constructor + field injection) by the {@link ComponentContainer};
 * this consumer fetches it and registers it. {@code WebsocketProcessor} routes incoming events via
 * {@link JavaWebsocketRegistry#dispatch}.
 */
@Component
@Order(600)
public class WebsocketClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketClassConsumer.class);

    private final ComponentContainer componentContainer;
    private final JavaWebsocketRegistry registry;

    @Autowired
    public WebsocketClassConsumer(ComponentContainer componentContainer, JavaWebsocketRegistry registry) {
        this.componentContainer = componentContainer;
        this.registry = registry;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Websocket.class) || WebsocketHandler.class.isAssignableFrom(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("Websocket handler [{}] was not instantiated by the bean container; skipped.", info.fqn());
            return;
        }

        if (instance instanceof WebsocketHandler handler) {
            if (type.isAnnotationPresent(Websocket.class) || hasCallbackAnnotation(type)) {
                LOGGER.error("[{}] mixes websocket styles — a WebsocketHandler must not also carry @Websocket or @OnX methods. "
                        + "Use one style or the other; skipped.", info.fqn());
                return;
            }
            registry.register(handler.endpoint(), instance);
            LOGGER.info("Java WebsocketHandler [{}] registered for endpoint '{}'.", info.fqn(), handler.endpoint());
            return;
        }

        Websocket ann = type.getAnnotation(Websocket.class);
        registry.register(ann.endpoint(), instance);
        LOGGER.info("Java @Websocket [{}] registered for endpoint '{}'.", info.fqn(), ann.endpoint());
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        Class<?> type = info.type();
        String endpoint = type.isAnnotationPresent(Websocket.class) ? type.getAnnotation(Websocket.class)
                                                                          .endpoint()
                : endpointOfHandler(info);
        if (endpoint != null) {
            registry.unregister(endpoint);
            LOGGER.info("Java websocket handler [{}] unregistered from endpoint '{}'.", info.fqn(), endpoint);
        }
    }

    private String endpointOfHandler(LoadedClass info) {
        return componentContainer.instanceOf(info.type())
                                 .filter(WebsocketHandler.class::isInstance)
                                 .map(instance -> ((WebsocketHandler) instance).endpoint())
                                 .orElse(null);
    }

    private static boolean hasCallbackAnnotation(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnOpen.class) || method.isAnnotationPresent(OnMessage.class)
                    || method.isAnnotationPresent(OnError.class) || method.isAnnotationPresent(OnClose.class)) {
                return true;
            }
        }
        return false;
    }
}
