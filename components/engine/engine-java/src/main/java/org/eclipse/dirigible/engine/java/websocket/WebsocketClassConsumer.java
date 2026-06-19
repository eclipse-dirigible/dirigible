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

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.net.Websocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client classes annotated with {@link Websocket} in the
 * {@link JavaWebsocketRegistry}. The handler instance is built (with constructor + field injection)
 * by the {@link ComponentContainer}; this consumer only fetches it and binds it to the endpoint.
 *
 * <p>
 * {@code WebsocketProcessor} in {@code engine-websockets} dispatches incoming events to the Java
 * handler before falling back to JS. The lifecycle callbacks may be supplied as
 * {@code org.eclipse.dirigible.sdk.net.WebsocketHandler}, via {@code @OnOpen}/{@code @OnMessage}/
 * {@code @OnError}/{@code @OnClose} method annotations, or by the legacy method-name convention —
 * see {@code WebsocketProcessor} for the dispatch precedence.
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
        return clazz.isAnnotationPresent(Websocket.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Websocket ann = info.type()
                            .getAnnotation(Websocket.class);

        Object instance = componentContainer.instanceOf(info.type())
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("@Websocket [{}] was not instantiated by the bean container; skipped.", info.fqn());
            return;
        }

        registry.register(ann.endpoint(), instance);
        LOGGER.info("Java @Websocket [{}] registered for endpoint '{}'.", info.fqn(), ann.endpoint());
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        Websocket ann = info.type()
                            .getAnnotation(Websocket.class);
        registry.unregister(ann.endpoint());
        LOGGER.info("Java @Websocket [{}] unregistered from endpoint '{}'.", info.fqn(), ann.endpoint());
    }
}
