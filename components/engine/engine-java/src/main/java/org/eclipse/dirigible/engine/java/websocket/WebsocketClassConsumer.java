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

import java.lang.reflect.Constructor;

import org.eclipse.dirigible.sdk.net.Websocket;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client classes annotated with {@link Websocket} in the
 * {@link JavaWebsocketRegistry}.
 *
 * <p>
 * {@code WebsocketProcessor} in {@code engine-websockets} optionally injects the registry and
 * dispatches incoming events to the Java handler before falling back to JS.
 *
 * <p>
 * The handler class may expose any combination of:
 * <ul>
 * <li>{@code onOpen()}</li>
 * <li>{@code onMessage(String message, String from)}</li>
 * <li>{@code onError(String error)}</li>
 * <li>{@code onClose()}</li>
 * </ul>
 * All methods are optional — missing ones are silently skipped by {@code WebsocketProcessor}.
 */
@Component
@Order(600)
public class WebsocketClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketClassConsumer.class);

    private final JavaWebsocketRegistry registry;

    @Autowired
    public WebsocketClassConsumer(JavaWebsocketRegistry registry) {
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

        Object instance = instantiate(info);
        if (instance == null) {
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

    private static Object instantiate(LoadedClass info) {
        try {
            Constructor<?> ctor = info.type()
                                      .getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to instantiate @Websocket class [{}]: {}", info.fqn(), e.getMessage(), e);
            return null;
        }
    }
}
