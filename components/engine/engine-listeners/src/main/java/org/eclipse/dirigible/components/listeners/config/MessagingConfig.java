/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import java.io.File;
import javax.sql.DataSource;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.openwire.OpenWireFormat;
import org.apache.activemq.store.PListStore;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.jdbc.JDBCPersistenceAdapter;
import org.apache.activemq.store.kahadb.plist.PListStoreImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * The Class MessagingConfig.
 */
@Configuration
class MessagingConfig {

    /** The Constant CONNECTOR_URL_ATTACH. */
    private static final String CONNECTOR_URL_ATTACH = "vm://localhost?create=false";

    /** The Constant CONNECTOR_URL. */
    private static final String CONNECTOR_URL = "vm://localhost";

    /** The Constant LOCATION_TEMP_STORE. */
    private static final String LOCATION_TEMP_STORE = "./target/dirigible/kahadb";

    /**
     * Creates the active MQ connection factory.
     *
     * @return the active MQ connection factory
     */
    @Bean
    ActiveMQConnectionFactory createActiveMQConnectionFactory() {
        return new ActiveMQConnectionFactory(CONNECTOR_URL_ATTACH);
    }

    /**
     * Creates the broker service.
     *
     * @param dataSource the data source
     * @return the broker service
     */
    @Bean("ActiveMQBroker")
    BrokerService createBrokerService(@Qualifier("SystemDB") DataSource dataSource) {
        try {
            BrokerService broker = new BrokerService();
            if (!broker.isStarted()) {
                if (Boolean.parseBoolean(
                        org.eclipse.dirigible.commons.config.Configuration.get("DIRIGIBLE_MESSAGING_USE_DEFAULT_DATABASE", "true"))) {
                    PersistenceAdapter persistenceAdapter = new JDBCPersistenceAdapter(dataSource, new OpenWireFormat());
                    broker.setPersistenceAdapter(persistenceAdapter);
                }
                broker.setPersistent(true);
                broker.setUseJmx(false);
                PListStore pListStore = new PListStoreImpl();
                pListStore.setDirectory(new File(LOCATION_TEMP_STORE));
                broker.setTempDataStore(pListStore);
                broker.addConnector(CONNECTOR_URL);

                broker.start();
            }

            return broker;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to init ActiveMQ broker", ex);
        }
    }

    /**
     * Creates the connection.
     *
     * @param connection the connection
     * @return the session
     */
    @Bean("ActiveMQSession")
    Session createSession(@Qualifier("ActiveMQConnection") Connection connection) {
        try {
            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to create session to ActiveMQ", ex);
        }
    }

    /**
     * Creates the connection.
     *
     * @param connectionArtifactsFactory the connection artifacts factory
     * @param loggingExceptionListener the logging exception listener
     * @return the connection
     */
    @Bean("ActiveMQConnection")
    @DependsOn("ActiveMQBroker")
    Connection createConnection(ActiveMQConnectionArtifactsFactory connectionArtifactsFactory,
            LoggingExceptionListener loggingExceptionListener) {
        return connectionArtifactsFactory.createConnection(loggingExceptionListener);
    }
}
