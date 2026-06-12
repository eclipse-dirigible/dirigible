/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.sdk.messaging.Listener;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * {@link JavaClassConsumer} that connects client classes annotated with {@link Listener} to
 * ActiveMQ queues or topics.
 *
 * <p>
 * Implementing the optional {@link MessageHandler} interface gives compile-time signature checking
 * and a direct, non-reflective dispatch path (Java's virtual dispatch goes straight to the impl,
 * including the default {@code onError} no-op). Classes that don't implement it still work — the
 * consumer falls back to looking up {@code onMessage(String)} (and the optional
 * {@code onError(String)}) by name and invoking them reflectively. A dedicated JMS Connection is
 * created for each registered listener and torn down on unload.
 */
@Component
@Order(500)
public class ListenerClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerClassConsumer.class);

    private final ActiveMQConnectionArtifactsFactory connectionFactory;

    /** fqn → open JMS Connection for teardown. */
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();

    @Autowired
    public ListenerClassConsumer(ActiveMQConnectionArtifactsFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Listener.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Listener ann = info.type()
                           .getAnnotation(Listener.class);

        Object instance = instantiate(info);
        if (instance == null) {
            return;
        }

        Dispatcher dispatcher;
        if (instance instanceof MessageHandler typed) {
            // Typed path: Java virtual dispatch lands directly on the impl's onMessage / onError.
            // The interface's default onError() is a no-op, so callers don't need to override it.
            dispatcher = new TypedDispatcher(typed);
        } else {
            Method onMessage;
            try {
                onMessage = info.type()
                                .getMethod("onMessage", String.class);
            } catch (NoSuchMethodException e) {
                LOGGER.error("@Listener class [{}] must implement MessageHandler or expose a public onMessage(String) method; skipped.",
                        info.fqn());
                return;
            }
            Method onError = findOptionalMethod(info.type(), "onError", String.class);
            dispatcher = new ReflectiveDispatcher(instance, onMessage, onError);
        }

        stopExisting(info.fqn());

        try {
            Connection connection = connectionFactory.createConnection(
                    ex -> LOGGER.error("[java-listener] JMS error for [{}]: {}", info.fqn(), ex.getMessage(), ex));
            Session session = connectionFactory.createSession(connection);

            Destination destination = ann.kind() == ListenerKind.TOPIC ? session.createTopic(ann.name()) : session.createQueue(ann.name());

            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(msg -> dispatch(msg, dispatcher, info.fqn()));

            connections.put(info.fqn(), connection);
            LOGGER.info("Java @Listener [{}] connected to {} '{}' ({} dispatch).", info.fqn(), ann.kind(), ann.name(),
                    instance instanceof MessageHandler ? "typed" : "reflective");
        } catch (JMSException e) {
            LOGGER.error("Failed to start listener for [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        stopExisting(info.fqn());
        LOGGER.info("Java @Listener [{}] disconnected.", info.fqn());
    }

    private void stopExisting(String fqn) {
        Connection old = connections.remove(fqn);
        if (old != null) {
            try {
                old.close();
            } catch (JMSException e) {
                LOGGER.warn("Failed to close JMS connection for [{}]: {}", fqn, e.getMessage());
            }
        }
    }

    private static Object instantiate(LoadedClass info) {
        try {
            Constructor<?> ctor = info.type()
                                      .getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to instantiate @Listener class [{}]: {}", info.fqn(), e.getMessage(), e);
            return null;
        }
    }

    private static Method findOptionalMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void dispatch(Message msg, Dispatcher dispatcher, String fqn) {
        if (!(msg instanceof TextMessage textMsg)) {
            LOGGER.warn("@Listener [{}] received a non-text message; ignored.", fqn);
            return;
        }
        String text;
        try {
            text = textMsg.getText();
        } catch (JMSException e) {
            LOGGER.error("@Listener [{}] failed to read text message: {}", fqn, e.getMessage(), e);
            dispatcher.onError(e.getMessage(), fqn);
            return;
        }
        try {
            dispatcher.onMessage(text);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            dispatcher.onError(cause.getMessage(), fqn);
        }
    }

    /** Abstraction over the typed and reflective callback paths so {@link #dispatch} stays uniform. */
    private interface Dispatcher {
        void onMessage(String text) throws Exception;

        void onError(String error, String fqn);
    }

    private record TypedDispatcher(MessageHandler handler) implements Dispatcher {

        @Override
        public void onMessage(String text) {
            handler.onMessage(text);
        }

        @Override
        public void onError(String error, String fqn) {
            try {
                handler.onError(error);
            } catch (RuntimeException ex) {
                LOGGER.error("@Listener [{}] onError() threw: {}", fqn, ex.getMessage(), ex);
            }
        }
    }

    private record ReflectiveDispatcher(Object instance, Method onMessage, Method onError) implements Dispatcher {

        @Override
        public void onMessage(String text) throws ReflectiveOperationException {
            onMessage.invoke(instance, text);
        }

        @Override
        public void onError(String error, String fqn) {
            if (onError == null) {
                LOGGER.error("@Listener [{}] onMessage() threw: {}", fqn, error);
                return;
            }
            try {
                onError.invoke(instance, error);
            } catch (ReflectiveOperationException ex) {
                LOGGER.error("@Listener [{}] onError() threw: {}", fqn, ex.getMessage(), ex);
            }
        }
    }
}
