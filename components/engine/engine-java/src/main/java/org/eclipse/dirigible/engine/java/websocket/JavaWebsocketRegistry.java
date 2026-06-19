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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.dirigible.sdk.net.OnClose;
import org.eclipse.dirigible.sdk.net.OnError;
import org.eclipse.dirigible.sdk.net.OnMessage;
import org.eclipse.dirigible.sdk.net.OnOpen;
import org.eclipse.dirigible.sdk.net.WebsocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runtime registry mapping WebSocket endpoint names to the Java handler instances registered via
 * {@link org.eclipse.dirigible.sdk.net.Websocket @Websocket}, plus the dispatch logic that routes
 * an inbound event to the right callback. The callback shape is resolved once at registration, in
 * this precedence:
 * <ol>
 * <li>{@link WebsocketHandler} — typed interface, direct virtual dispatch;</li>
 * <li>{@code @OnOpen}/{@code @OnMessage}/{@code @OnError}/{@code @OnClose} annotated methods;</li>
 * <li>methods named {@code onOpen}/{@code onMessage}/{@code onError}/{@code onClose} — the legacy
 * reflective fallback.</li>
 * </ol>
 *
 * <p>
 * {@code WebsocketProcessor} in {@code engine-websockets} resolves this bean reflectively and calls
 * {@link #dispatch(String, String, String, String, String)} so that module stays free of an
 * {@code engine-java} dependency.
 */
@Component
public class JavaWebsocketRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaWebsocketRegistry.class);

    private final ConcurrentMap<String, Endpoint> handlers = new ConcurrentHashMap<>();

    /**
     * Register a Java websocket handler instance for the given endpoint name.
     *
     * @param endpoint the endpoint name (matches
     *        {@link org.eclipse.dirigible.sdk.net.Websocket#endpoint()})
     * @param instance the handler instance
     */
    public void register(String endpoint, Object instance) {
        handlers.put(endpoint, Endpoint.resolve(instance));
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
        Endpoint endpoint0 = handlers.get(endpoint);
        return endpoint0 == null ? null : endpoint0.instance();
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

    /**
     * Dispatch a WebSocket lifecycle event to the registered Java handler.
     *
     * @param endpoint the endpoint name
     * @param method one of {@code onopen} / {@code onmessage} / {@code onclose} / {@code onerror}
     * @param message the inbound text payload (for {@code onmessage})
     * @param from the originating session identifier (for {@code onmessage})
     * @param error the error text (for {@code onerror})
     * @return for {@code onmessage}, the handler's return value as text (or {@code ""}); otherwise
     *         {@code null}
     */
    public Object dispatch(String endpoint, String method, String message, String from, String error) {
        Endpoint handler = handlers.get(endpoint);
        if (handler == null) {
            return null;
        }
        try {
            return switch (method == null ? "" : method) {
                case "onmessage" -> handler.onMessage(message, from);
                case "onopen" -> {
                    handler.onOpen();
                    yield null;
                }
                case "onclose" -> {
                    handler.onClose();
                    yield null;
                }
                case "onerror" -> {
                    handler.onError(error);
                    yield null;
                }
                default -> {
                    LOGGER.warn("Unknown websocket method [{}] for endpoint [{}]", method, endpoint);
                    yield null;
                }
            };
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOGGER.error("Java @Websocket handler [{}] threw on [{}]: {}", endpoint, method, cause.getMessage(), cause);
            return null;
        }
    }

    /** A resolved handler: the instance plus the callback methods (or the typed interface). */
    private record Endpoint(Object instance, WebsocketHandler typed, Method openMethod, Method messageMethod, Method errorMethod,
            Method closeMethod) {

        static Endpoint resolve(Object instance) {
            if (instance instanceof WebsocketHandler typed) {
                return new Endpoint(instance, typed, null, null, null, null);
            }
            Class<?> type = instance.getClass();
            return new Endpoint(instance, null, find(type, OnOpen.class, "onOpen"), findMessage(type),
                    find(type, OnError.class, "onError", String.class), find(type, OnClose.class, "onClose"));
        }

        Object onMessage(String message, String from) throws ReflectiveOperationException {
            if (typed != null) {
                typed.onMessage(message, from);
                return "";
            }
            if (messageMethod == null) {
                return null;
            }
            Object result = messageMethod.getParameterCount() == 2 ? messageMethod.invoke(instance, message, from)
                    : messageMethod.invoke(instance, message);
            return result != null ? result.toString() : "";
        }

        void onOpen() throws ReflectiveOperationException {
            if (typed != null) {
                typed.onOpen();
            } else if (openMethod != null) {
                openMethod.invoke(instance);
            }
        }

        void onError(String error) throws ReflectiveOperationException {
            if (typed != null) {
                typed.onError(error);
            } else if (errorMethod != null) {
                errorMethod.invoke(instance, error);
            }
        }

        void onClose() throws ReflectiveOperationException {
            if (typed != null) {
                typed.onClose();
            } else if (closeMethod != null) {
                closeMethod.invoke(instance);
            }
        }

        private static Method find(Class<?> type, Class<? extends Annotation> annotation, String name, Class<?>... params) {
            for (Method method : type.getMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            try {
                return type.getMethod(name, params);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        private static Method findMessage(Class<?> type) {
            for (Method method : type.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            try {
                return type.getMethod("onMessage", String.class, String.class);
            } catch (NoSuchMethodException e) {
                try {
                    return type.getMethod("onMessage", String.class);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
        }
    }
}
