/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.rabbitmq;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;

import nl.altindag.log.LogCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RabbitMQFacadeTest {

    LogCaptor logCaptor = LogCaptor.forClass(RabbitMQFacade.class);
    private static final String message = "testMessage";
    private static final String queue = "test-queue";


    @Before
    public void setUp() {
        RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.8.19-alpine");
        rabbit.start();

        String host = rabbit.getHost();
        Integer port = rabbit.getFirstMappedPort();
        Configuration.set("DIRIGIBLE_RABBITMQ_CLIENT_URI", host + ":" + port);
    }

    @Test
    public void send() {
        logCaptor.setLogLevelToInfo();

        RabbitMQFacade.send(queue, message);
        assertEquals(logCaptor.getInfoLogs()
                              .get(0),
                "Sent: " + "'" + message + "'" + " to [" + queue + "]");

    }

    @Test
    public void rabbitMQIntegration() {
        logCaptor.setLogLevelToInfo();

        RabbitMQFacade.startListening(queue, "rabbitmq/test-handler");
        assertEquals(logCaptor.getInfoLogs()
                              .get(0),
                "RabbitMQ receiver created for [" + queue + "]");

        RabbitMQFacade.stopListening(queue, "rabbitmq/test-handler");
        assertEquals(logCaptor.getInfoLogs()
                              .get(1),
                "RabbitMQ receiver stopped for [" + queue + "]");
    }
}
