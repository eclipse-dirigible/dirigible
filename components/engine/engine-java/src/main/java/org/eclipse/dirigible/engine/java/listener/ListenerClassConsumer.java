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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.service.TenantPropertyManager;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.messaging.Listener;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
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
 * {@link JavaClassConsumer} that connects client listeners to ActiveMQ queues or topics. Two
 * styles, never mixed on one class:
 * <ul>
 * <li><b>self-describing interface</b> — a {@code @Component} bean implementing
 * {@link MessageHandler}, which supplies its own {@code destination()} / {@code kind()} and
 * {@code onMessage(String)};</li>
 * <li><b>method level</b> — public {@code void m(String)} methods annotated
 * {@link Listener @Listener} on a client bean, Spring's {@code @JmsListener}-on-a-method style; a
 * bean may host several.</li>
 * </ul>
 * The bean is built (with constructor + field injection) by the {@link ComponentContainer}; this
 * consumer fetches it and opens a JMS connection per subscription, tearing them down on unload.
 */
@Component
@Order(500)
public class ListenerClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerClassConsumer.class);

    private final ComponentContainer componentContainer;
    private final ActiveMQConnectionArtifactsFactory connectionFactory;
    private final TenantContext tenantContext;
    private final TenantPropertyManager tenantPropertyManager;
    private final Tenant defaultTenant;

    /** fqn → open JMS Connections (one per subscription) for teardown. */
    private final ConcurrentMap<String, List<Connection>> connections = new ConcurrentHashMap<>();

    @Autowired
    public ListenerClassConsumer(ComponentContainer componentContainer, ActiveMQConnectionArtifactsFactory connectionFactory,
            TenantContext tenantContext, TenantPropertyManager tenantPropertyManager, @DefaultTenant Tenant defaultTenant) {
        this.componentContainer = componentContainer;
        this.connectionFactory = connectionFactory;
        this.tenantContext = tenantContext;
        this.tenantPropertyManager = tenantPropertyManager;
        this.defaultTenant = defaultTenant;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return MessageHandler.class.isAssignableFrom(clazz) || hasListenerMethod(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("Listener [{}] was not instantiated as a bean — a MessageHandler and a @Listener method both require "
                    + "the class to be a @Component; skipped.", info.fqn());
            return;
        }

        boolean messageHandler = instance instanceof MessageHandler;
        boolean methodLevel = hasListenerMethod(type);
        if (messageHandler && methodLevel) {
            LOGGER.error("[{}] mixes listener styles — it implements MessageHandler and also declares @Listener methods. "
                    + "Use one style or the other; skipped.", info.fqn());
            return;
        }

        stopExisting(info.fqn());
        List<Connection> opened = new ArrayList<>();

        if (messageHandler) {
            MessageHandler handler = (MessageHandler) instance;
            subscribe(opened, handler.destination(), handler.kind(), new TypedDispatcher(handler), info.fqn());
        } else {
            for (Method method : type.getDeclaredMethods()) {
                Listener annotation = method.getAnnotation(Listener.class);
                if (annotation == null) {
                    continue;
                }
                if (!isEligibleMethod(method)) {
                    LOGGER.error("@Listener method [{}#{}] must be public and take a single String parameter; skipped.", info.fqn(),
                            method.getName());
                    continue;
                }
                method.setAccessible(true);
                String label = info.fqn() + "#" + method.getName();
                subscribe(opened, annotation.name(), annotation.kind(), new MethodDispatcher(instance, method), label);
            }
        }

        if (opened.isEmpty()) {
            LOGGER.warn("Listener [{}] produced no subscription.", info.fqn());
            return;
        }
        connections.put(info.fqn(), opened);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        stopExisting(info.fqn());
        LOGGER.info("Java @Listener [{}] disconnected.", info.fqn());
    }

    private void subscribe(List<Connection> opened, String destinationName, ListenerKind kind, Dispatcher dispatcher, String label) {
        try {
            Connection connection = connectionFactory.createConnection(
                    ex -> LOGGER.error("[java-listener] JMS error for [{}]: {}", label, ex.getMessage(), ex));
            Session session = connectionFactory.createSession(connection);
            Destination destination =
                    kind == ListenerKind.TOPIC ? session.createTopic(destinationName) : session.createQueue(destinationName);
            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(msg -> dispatch(msg, dispatcher, label));
            opened.add(connection);
            LOGGER.info("Java @Listener [{}] connected to {} '{}'.", label, kind, destinationName);
        } catch (JMSException e) {
            LOGGER.error("Failed to start listener for [{}]: {}", label, e.getMessage(), e);
        }
    }

    private void stopExisting(String fqn) {
        List<Connection> old = connections.remove(fqn);
        if (old != null) {
            for (Connection connection : old) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    LOGGER.warn("Failed to close JMS connection for [{}]: {}", fqn, e.getMessage(), e);
                }
            }
        }
    }

    private static boolean hasListenerMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listener.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class
                && !method.isSynthetic();
    }

    private void dispatch(Message msg, Dispatcher dispatcher, String label) {
        if (!(msg instanceof TextMessage textMsg)) {
            LOGGER.warn("@Listener [{}] received a non-text message; ignored.", label);
            return;
        }
        String text;
        try {
            text = textMsg.getText();
        } catch (JMSException e) {
            LOGGER.error("@Listener [{}] failed to read text message: {}", label, e.getMessage(), e);
            dispatcher.onError(e.getMessage(), label);
            return;
        }
        // The message arrives on a broker thread with no tenant context. Recover the originating
        // tenant the producer stamped on it and re-establish it for the handler, the same way the
        // built-in asynchronous listener does - otherwise handler code that touches tenant-scoped
        // services (DB, BPM via Process.start, ...) fails with "current tenant is not initialized".
        // Messages from outside the platform producer carry no tenant; fall back to the default
        // tenant so the handler still runs within a valid context.
        String tenantId;
        try {
            tenantId = tenantPropertyManager.getCurrentTenantId(msg);
        } catch (JMSException | RuntimeException e) {
            LOGGER.debug("@Listener [{}] message carries no tenant; using the default tenant. {}", label, e.getMessage(), e);
            tenantId = defaultTenant.getId();
        }
        try {
            tenantContext.execute(tenantId, () -> {
                dispatcher.onMessage(text);
                return null;
            });
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            dispatcher.onError(cause.getMessage(), label);
        }
    }

    /** Abstraction over the typed (MessageHandler) and method-level callback paths. */
    private interface Dispatcher {
        void onMessage(String text) throws Exception;

        void onError(String error, String label);
    }

    private record TypedDispatcher(MessageHandler handler) implements Dispatcher {

        @Override
        public void onMessage(String text) {
            handler.onMessage(text);
        }

        @Override
        public void onError(String error, String label) {
            try {
                handler.onError(error);
            } catch (RuntimeException ex) {
                LOGGER.error("@Listener [{}] onError() threw: {}", label, ex.getMessage(), ex);
            }
        }
    }

    private record MethodDispatcher(Object instance, Method method) implements Dispatcher {

        @Override
        public void onMessage(String text) throws ReflectiveOperationException {
            method.invoke(instance, text);
        }

        @Override
        public void onError(String error, String label) {
            LOGGER.error("@Listener [{}] handler threw: {}", label, error);
        }
    }
}
