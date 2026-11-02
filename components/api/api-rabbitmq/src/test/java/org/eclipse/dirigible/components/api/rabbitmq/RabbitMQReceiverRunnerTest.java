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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;

/**
 * Stopping a receiver releases only what the receiver owns - its consumer, channel and connection -
 * and leaves the queue, which other receivers may consume and which may hold pending messages
 * (#8210).
 */
class RabbitMQReceiverRunnerTest {

    private static final String QUEUE = "orders";

    private static final String CONSUMER_TAG = "amq.ctag-receiver";

    private final Connection connection = mock(Connection.class);

    private final Channel channel = mock(Channel.class);

    private final RabbitMQReceiverRunner runner = new RabbitMQReceiverRunner(connection, channel, QUEUE, message -> {
    });

    @Test
    void stoppingCancelsTheOwnConsumerAndKeepsTheQueue() throws Exception {
        when(channel.basicConsume(eq(QUEUE), anyBoolean(), any(Consumer.class))).thenReturn(CONSUMER_TAG);

        Thread receiver = startAndStop();

        InOrder order = inOrder(channel, connection);
        order.verify(channel)
             .basicCancel(CONSUMER_TAG);
        order.verify(channel)
             .close();
        order.verify(connection)
             .close();
        assertQueueKept();
        assertFalse(receiver.isAlive(), "the receiver thread must end once stopped");
    }

    @Test
    void aFailedCancelStillClosesTheChannelAndTheConnection() throws Exception {
        when(channel.basicConsume(eq(QUEUE), anyBoolean(), any(Consumer.class))).thenReturn(CONSUMER_TAG);
        doThrow(new IOException("cancel refused")).when(channel)
                                                  .basicCancel(CONSUMER_TAG);
        doThrow(new IOException("already closed")).when(channel)
                                                  .close();

        startAndStop();

        verify(connection).close();
        assertQueueKept();
    }

    @Test
    void aReceiverThatNeverConsumedCancelsNothingAndStillCloses() throws Exception {
        when(channel.basicConsume(eq(QUEUE), anyBoolean(), any(Consumer.class))).thenThrow(new IOException("access refused"));

        runner.run();

        verify(channel, never()).basicCancel(anyString());
        verify(channel).close();
        verify(connection).close();
        assertQueueKept();
    }

    private Thread startAndStop() throws Exception {
        Thread receiver = new Thread(runner);
        receiver.start();
        verify(channel, timeout(TimeUnit.SECONDS.toMillis(10))).basicConsume(eq(QUEUE), anyBoolean(), any(Consumer.class));
        runner.stop();
        receiver.join(TimeUnit.SECONDS.toMillis(10));
        return receiver;
    }

    private void assertQueueKept() throws Exception {
        verify(channel, never()).queueDelete(anyString());
        verify(channel, never()).queueDelete(anyString(), anyBoolean(), anyBoolean());
        verify(channel, never()).queuePurge(anyString());
    }
}
