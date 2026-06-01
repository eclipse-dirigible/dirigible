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
import org.eclipse.dirigible.engine.java.annotations.Listener;
import org.eclipse.dirigible.engine.java.annotations.ListenerKind;
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
 * The annotated class must expose a public {@code onMessage(String message)} method and optionally
 * a public {@code onError(String error)} method. The class is instantiated once per load cycle; a
 * dedicated JMS Connection is created for each registered listener and torn down on unload.
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

        Method onMessage;
        try {
            onMessage = info.type()
                            .getMethod("onMessage", String.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("@Listener class [{}] must expose a public onMessage(String) method; skipped.", info.fqn());
            return;
        }

        Method onError = findOptionalMethod(info.type(), "onError", String.class);

        Object instance = instantiate(info);
        if (instance == null) {
            return;
        }

        stopExisting(info.fqn());

        try {
            Connection connection = connectionFactory.createConnection(
                    ex -> LOGGER.error("[java-listener] JMS error for [{}]: {}", info.fqn(), ex.getMessage(), ex));
            Session session = connectionFactory.createSession(connection);

            Destination destination = ann.kind() == ListenerKind.TOPIC ? session.createTopic(ann.name()) : session.createQueue(ann.name());

            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(msg -> dispatch(msg, onMessage, onError, instance, info.fqn()));

            connections.put(info.fqn(), connection);
            LOGGER.info("Java @Listener [{}] connected to {} '{}'.", info.fqn(), ann.kind(), ann.name());
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

    private static void dispatch(Message msg, Method onMessage, Method onError, Object instance, String fqn) {
        if (!(msg instanceof TextMessage textMsg)) {
            LOGGER.warn("@Listener [{}] received a non-text message; ignored.", fqn);
            return;
        }
        try {
            String text = textMsg.getText();
            onMessage.invoke(instance, text);
        } catch (ReflectiveOperationException | JMSException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (onError != null) {
                try {
                    onError.invoke(instance, cause.getMessage());
                } catch (ReflectiveOperationException ex) {
                    LOGGER.error("@Listener [{}] onError() threw: {}", fqn, ex.getMessage(), ex);
                }
            } else {
                LOGGER.error("@Listener [{}] onMessage() threw: {}", fqn, cause.getMessage(), cause);
            }
        }
    }
}
