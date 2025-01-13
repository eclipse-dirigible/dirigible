/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.ftp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.ftp.server.ApacheMinaFtpEvent;
import org.springframework.integration.ftp.server.ApacheMinaFtplet;
import org.springframework.messaging.MessageChannel;

/**
 * The Class IntegrationConfiguration.
 */
@Configuration
public class IntegrationConfiguration {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(IntegrationConfiguration.class);

    /**
     * Apache mina ftplet.
     *
     * @return the apache mina ftplet
     */
    @Bean
    ApacheMinaFtplet apacheMinaFtplet() {
        return new ApacheMinaFtplet();
    }

    /**
     * Events channel.
     *
     * @return the message channel
     */
    @Bean
    MessageChannel eventsChannel() {
        return MessageChannels.direct()
                              .getObject();
    }

    /**
     * Integration flow.
     *
     * @return the integration flow
     */
    @Bean
    IntegrationFlow integrationFlow() {
        return IntegrationFlow.from(this.eventsChannel())
                              .handle((GenericHandler<ApacheMinaFtpEvent>) (apacheMinaFtpEvent, messageHeaders) -> {
                                  logger.info("new event: " + apacheMinaFtpEvent.getClass()
                                                                                .getName()
                                          + ':' + apacheMinaFtpEvent.getSession());
                                  return null;
                              })
                              .get();
    }

    /**
     * Application event listening message producer.
     *
     * @return the application event listening message producer
     */
    @Bean
    ApplicationEventListeningMessageProducer applicationEventListeningMessageProducer() {
        var producer = new ApplicationEventListeningMessageProducer();
        producer.setEventTypes(ApacheMinaFtpEvent.class);
        producer.setOutputChannel(eventsChannel());
        return producer;
    }

}
