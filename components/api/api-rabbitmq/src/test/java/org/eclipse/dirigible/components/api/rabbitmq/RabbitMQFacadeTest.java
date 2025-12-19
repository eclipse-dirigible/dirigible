/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.rabbitmq;

import nl.altindag.log.LogCaptor;
import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisabledOnOs(OS.WINDOWS)
@Testcontainers
public class RabbitMQFacadeTest {

    private static final String message = "testMessage";
    private static final String queue = "test-queue";

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.8.19-alpine");

    LogCaptor logCaptor = LogCaptor.forClass(RabbitMQFacade.class);

    @BeforeAll
    static void setUp() {
        Configuration.set("DIRIGIBLE_RABBITMQ_CLIENT_URI", rabbit.getHost() + ":" + rabbit.getFirstMappedPort());
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
