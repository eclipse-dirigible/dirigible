/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.broker.BrokerService;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationStoppedEventListeners;
import org.eclipse.dirigible.components.listeners.service.ListenersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving closeActiveMQResourcesApplication events. The class that is
 * interested in processing a closeActiveMQResourcesApplication event implements this interface, and
 * the object created with that class is registered with a component using the component's
 * closeActiveMQResourcesApplication event occurs, that object's appropriate method is invoked.
 */
@Order(ApplicationStoppedEventListeners.ACTIVE_MQ_CLEANUP)
@Component
class CloseActiveMQResourcesApplicationListener implements ApplicationListener<ApplicationEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseActiveMQResourcesApplicationListener.class);

    /** The name of the connection bean, which against a shared message store attaches on first use. */
    private static final String CONNECTION_BEAN = "ActiveMQConnection";

    /** The broker, absent when the deployment uses an external broker. */
    private final ObjectProvider<BrokerService> brokerProvider;

    /** The connection. */
    private final Connection connection;

    /** The session. */
    private final Session session;

    /** The listeners manager. */
    private final ListenersManager listenersManager;

    /** The bean factory, asked whether the messaging ever attached. */
    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * Instantiates a new close active MQ resources application listener.
     *
     * @param brokerProvider provides the embedded broker, which an external-broker deployment does not
     *        have
     * @param connection the connection, resolved on first use
     * @param session the session, resolved on first use
     * @param listenersManager the listeners manager
     * @param beanFactory the bean factory, which knows whether the connection was ever resolved
     */
    @Autowired
    CloseActiveMQResourcesApplicationListener(ObjectProvider<BrokerService> brokerProvider,
            @Lazy @Qualifier("ActiveMQConnection") Connection connection, @Lazy @Qualifier("ActiveMQSession") Session session,
            ListenersManager listenersManager, ConfigurableListableBeanFactory beanFactory) {
        this.brokerProvider = brokerProvider;
        this.connection = connection;
        this.session = session;
        this.listenersManager = listenersManager;
        this.beanFactory = beanFactory;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (isApplicableEvent(event)) {
            closeResources(event);
        }
    }

    /**
     * Checks if is applicable event.
     *
     * @param event the event
     * @return true, if is applicable event
     */
    private boolean isApplicableEvent(ApplicationEvent event) {
        return event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent;
    }

    /**
     * Close resources.
     *
     * @param event the event
     */
    private void closeResources(ApplicationEvent event) {
        LOGGER.info("Closing ActiveMQ resources due to event {}", event);
        stopListeners();
        if (isAttached()) {
            closeSession();
            closeConnection();
        }
        stopBroker();
    }

    /**
     * Whether the messaging ever attached. Against a shared message store the connection and the
     * session are opened on first use, so an instance that spent its life waiting for the lease has
     * none to close - and resolving them here would open a connection purely to close it, or hang the
     * shutdown against a broker that is still not the master.
     *
     * @return true, if the connection was resolved
     */
    private boolean isAttached() {
        return beanFactory.containsSingleton(CONNECTION_BEAN);
    }

    /**
     * Stop listeners.
     */
    private void stopListeners() {
        try {
            listenersManager.stopListeners();
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to stop listeners", ex);
        }
    }

    /**
     * Close session.
     */
    private void closeSession() {
        try {
            session.close();
        } catch (RuntimeException | JMSException ex) {
            LOGGER.warn("Failed to close session [{}]", session, ex);
        }
    }

    /**
     * Close connection.
     */
    private void closeConnection() {
        try {
            connection.close();
        } catch (RuntimeException | JMSException ex) {
            LOGGER.warn("Failed to close connection [{}]", connection, ex);
        }
    }

    /**
     * Stop broker. There is none to stop when the messaging rides an external broker - the connection
     * and the session closed above are all this deployment owns.
     */
    private void stopBroker() {
        BrokerService broker = brokerProvider.getIfAvailable();
        if (null == broker) {
            LOGGER.debug("No embedded ActiveMQ broker to stop - the messaging uses an external broker");
            return;
        }
        try {
            broker.stop();
        } catch (Exception ex) {
            LOGGER.warn("Failed to close broker {}", broker, ex);
        }
    }
}
