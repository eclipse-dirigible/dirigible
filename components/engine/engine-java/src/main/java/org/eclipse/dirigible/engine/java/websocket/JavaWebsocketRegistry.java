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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * Runtime registry mapping WebSocket endpoint names to the Java handler instances registered via
 * {@link org.eclipse.dirigible.sdk.net.Websocket @Websocket}.
 *
 * <p>
 * {@code WebsocketProcessor} in {@code engine-websockets} optionally injects this bean and routes
 * incoming events to Java handlers before falling back to the JS handler lookup.
 */
@Component
public class JavaWebsocketRegistry {

    private final ConcurrentMap<String, Object> handlers = new ConcurrentHashMap<>();

    /**
     * Register a Java websocket handler instance for the given endpoint name.
     *
     * @param endpoint the endpoint name (matches the value of
     *        {@link org.eclipse.dirigible.sdk.net.Websocket#endpoint()})
     * @param instance the handler instance
     */
    public void register(String endpoint, Object instance) {
        handlers.put(endpoint, instance);
    }

    /**
     * Remove the handler registered for {@code endpoint}.
     *
     * @param endpoint the endpoint name
     */
    public void unregister(String endpoint) {
        handlers.remove(endpoint);
    }

    /**
     * Returns the handler instance for the given endpoint, or {@code null} if none is registered.
     *
     * @param endpoint the endpoint name
     * @return the handler instance, or {@code null}
     */
    public Object get(String endpoint) {
        return handlers.get(endpoint);
    }

    /**
     * Returns {@code true} if a Java handler is registered for {@code endpoint}.
     *
     * @param endpoint the endpoint name
     * @return {@code true} if a handler is present
     */
    public boolean contains(String endpoint) {
        return handlers.containsKey(endpoint);
    }
}
