/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.base.script.ScriptExecutor;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Tag("testcontainers")
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

        // The handler is a script, so listening needs a script engine to hand the messages to.
        new ApplicationContextRunner().withUserConfiguration(BeanProvider.class)
                                      .withBean(ScriptExecutor.class, () -> mock(ScriptExecutor.class))
                                      .run(context -> RabbitMQFacade.startListening(queue, "rabbitmq/test-handler"));
        assertEquals(logCaptor.getInfoLogs()
                              .get(0),
                "RabbitMQ receiver created for [" + queue + "]");

        RabbitMQFacade.stopListening(queue, "rabbitmq/test-handler");
        assertEquals(logCaptor.getInfoLogs()
                              .get(1),
                "RabbitMQ receiver stopped for [" + queue + "]");
    }

    /** A message reaches a handler script through the messaging wrapper, as it always did. */
    @Test
    public void aScriptHandlerReceivesTheMessagesThroughItsWrapper() throws Exception {
        String scriptQueue = "script-handler-queue";
        String handler = "rabbitmq/test-handler";
        ScriptExecutor scriptExecutor = mock(ScriptExecutor.class);
        new ApplicationContextRunner().withUserConfiguration(BeanProvider.class)
                                      .withBean(ScriptExecutor.class, () -> scriptExecutor)
                                      .run(context -> RabbitMQFacade.startListening(scriptQueue, handler));
        try {
            awaitConsumers(scriptQueue, 1);
            RabbitMQFacade.send(scriptQueue, message);

            verify(scriptExecutor, timeout(10_000)).execute(eq("messaging"), eq("/wrappers/onMessage.js"), isNull(),
                    argThat(context -> handler.equals(context.get("handler")) && message.equals(context.get("message"))), eq(false));
        } finally {
            RabbitMQFacade.stopListening(scriptQueue, handler);
        }
    }

    /** A Java listener consumes a queue with no script engine deployed at all (#8017). */
    @Test
    public void aJavaListenerReceivesTheMessagesOfItsQueue() throws Exception {
        String javaQueue = "java-listener-queue";
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        RabbitMQMessageListener listener = received::add;

        RabbitMQFacade.startListening(javaQueue, listener);
        try {
            awaitConsumers(javaQueue, 1);
            RabbitMQFacade.send(javaQueue, "first");
            RabbitMQFacade.send(javaQueue, "second");

            assertEquals("first", received.poll(10, TimeUnit.SECONDS));
            assertEquals("second", received.poll(10, TimeUnit.SECONDS));
        } finally {
            RabbitMQFacade.stopListening(javaQueue, listener);
        }
    }

    @Test
    public void aJavaListenerIsToldWhyItsHandlingFailed() throws Exception {
        String failingQueue = "failing-listener-queue";
        BlockingQueue<String> errors = new LinkedBlockingQueue<>();
        RabbitMQMessageListener listener = new RabbitMQMessageListener() {

            @Override
            public void onMessage(String message) {
                throw new IllegalStateException("cannot handle " + message);
            }

            @Override
            public void onError(String error) {
                errors.add(error);
            }
        };

        RabbitMQFacade.startListening(failingQueue, listener);
        try {
            awaitConsumers(failingQueue, 1);
            RabbitMQFacade.send(failingQueue, message);

            assertEquals("cannot handle " + message, errors.poll(10, TimeUnit.SECONDS));
        } finally {
            RabbitMQFacade.stopListening(failingQueue, listener);
        }
    }

    /**
     * A receiver registers one consumer and keeps it. It used to register a new one in a loop until it
     * was stopped - thousands a second, each of them a broker round trip.
     */
    @Test
    public void aReceiverRegistersOneConsumer() throws Exception {
        String loneQueue = "lone-consumer-queue";
        RabbitMQMessageListener listener = received -> {
        };

        RabbitMQFacade.startListening(loneQueue, listener);
        try {
            awaitConsumers(loneQueue, 1);
            Thread.sleep(1000);
            assertEquals(1, consumerCount(loneQueue), "the receiver must keep a single consumer");
        } finally {
            RabbitMQFacade.stopListening(loneQueue, listener);
        }
    }

    @Test
    public void aStoppedJavaListenerReceivesNoMore() throws Exception {
        String stoppedQueue = "stopped-listener-queue";
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        RabbitMQMessageListener listener = received::add;
        RabbitMQFacade.startListening(stoppedQueue, listener);
        awaitConsumers(stoppedQueue, 1);

        RabbitMQFacade.stopListening(stoppedQueue, listener);
        awaitConsumers(stoppedQueue, 0);
        RabbitMQFacade.send(stoppedQueue, message);

        assertNull(received.poll(2, TimeUnit.SECONDS));
    }

    /**
     * Stopping one receiver leaves the queue, the other receivers consuming it and the messages pending
     * in it (#8210) - the queue used to be deleted with every stop.
     */
    @Test
    public void stoppingAReceiverKeepsTheQueueItsMessagesAndTheOtherConsumers() throws Exception {
        String sharedQueue = "shared-consumer-queue";
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        RabbitMQMessageListener stopping = ignored -> {
        };
        RabbitMQMessageListener staying = received::add;
        RabbitMQFacade.startListening(sharedQueue, stopping);
        RabbitMQFacade.startListening(sharedQueue, staying);
        try {
            awaitConsumers(sharedQueue, 2);

            RabbitMQFacade.stopListening(sharedQueue, stopping);
            awaitConsumers(sharedQueue, 1);
            RabbitMQFacade.send(sharedQueue, message);

            assertEquals(message, received.poll(10, TimeUnit.SECONDS), "the remaining consumer must keep receiving");
        } finally {
            RabbitMQFacade.stopListening(sharedQueue, staying);
        }

        // passively, and published without a declaration: neither recreates a deleted queue
        awaitExistingQueue(sharedQueue, queueState -> queueState.getConsumerCount() == 0, "no consumer");
        try (Connection connection = adminConnection(); Channel channel = connection.createChannel()) {
            channel.basicPublish("", sharedQueue, null, message.getBytes(StandardCharsets.UTF_8));
        }
        awaitExistingQueue(sharedQueue, queueState -> queueState.getMessageCount() == 1, "one pending message");
    }

    /**
     * Waits until the queue, which must exist all along, reaches the given state - declared passively,
     * so that a deleted queue fails instead of being recreated.
     */
    private static void awaitExistingQueue(String queue, Predicate<AMQP.Queue.DeclareOk> state, String description) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
        while (!state.test(declarePassively(queue))) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Queue [" + queue + "] did not reach " + description);
            }
            Thread.sleep(100);
        }
    }

    private static AMQP.Queue.DeclareOk declarePassively(String queue) throws Exception {
        try (Connection connection = adminConnection(); Channel channel = connection.createChannel()) {
            return channel.queueDeclarePassive(queue);
        }
    }

    /**
     * Waits until the queue has the given number of consumers - a receiver starts on a thread of its
     * own.
     */
    private static void awaitConsumers(String queue, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15);
        while (consumerCount(queue) != expected) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Queue [" + queue + "] did not reach " + expected + " consumer(s)");
            }
            Thread.sleep(100);
        }
    }

    /** The consumers of a queue - none when the queue does not exist (yet, or any more). */
    private static int consumerCount(String queue) throws Exception {
        try (Connection connection = adminConnection(); Channel channel = connection.createChannel()) {
            return channel.queueDeclare(queue, false, false, false, Map.of())
                          .getConsumerCount();
        }
    }

    private static Connection adminConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        factory.setUsername(rabbit.getAdminUsername());
        factory.setPassword(rabbit.getAdminPassword());
        return factory.newConnection();
    }
}
