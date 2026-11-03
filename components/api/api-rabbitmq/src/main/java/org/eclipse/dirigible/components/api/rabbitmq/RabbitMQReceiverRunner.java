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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

/**
 * Consumes a queue on behalf of a listener - a handler script's or a Java one - until it is
 * stopped, then cancels its own consumer and closes its channel and connection. The queue itself
 * stays: other receivers - of this instance or of another one bound to the same broker - may
 * consume it, and it may hold messages not delivered yet (#8210).
 */
public class RabbitMQReceiverRunner implements Runnable {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQReceiverRunner.class);

    /**
     * The connection.
     */
    private final Connection connection;

    /**
     * The channel.
     */
    private final Channel channel;

    /**
     * The queue.
     */
    private final String queue;

    /**
     * The listener the messages are handed to.
     */
    private final RabbitMQMessageListener listener;

    /**
     * Released by {@link #stop()}.
     */
    private final CountDownLatch stopped = new CountDownLatch(1);

    /**
     * Instantiates a new rabbit MQ receiver runner.
     *
     * @param connection the connection
     * @param channel the channel
     * @param queue the queue
     * @param listener the listener the messages are handed to
     */
    public RabbitMQReceiverRunner(Connection connection, Channel channel, String queue, RabbitMQMessageListener listener) {
        this.connection = connection;
        this.channel = channel;
        this.queue = queue;
        this.listener = listener;
    }

    /**
     * Start the consumer and keep it until {@link #stop()}. The consumer is registered once - the
     * broker hands it every message, on the channel's delivery thread.
     */
    @Override
    public void run() {
        String consumerTag = null;
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Starting a RabbitMQ receiver for: " + this.queue);
            }
            channel.queueDeclare(queue, false, false, false, null);
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    deliver(new String(body, StandardCharsets.UTF_8));
                }
            };
            consumerTag = channel.basicConsume(queue, true, consumer);
            stopped.await();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            logger.warn("The RabbitMQ receiver for [{}] was interrupted", queue, e);
        } finally {
            release(consumerTag);
        }
    }

    /**
     * Cancels this receiver's consumer, when it was registered, and closes the channel and then the
     * connection - each step on its own, so a failing one does not leave the next behind. The queue is
     * never deleted.
     *
     * @param consumerTag the tag of the registered consumer, {@code null} when none was registered
     */
    private void release(String consumerTag) {
        if (consumerTag != null) {
            try {
                channel.basicCancel(consumerTag);
            } catch (Exception e) {
                logger.error("Failed to cancel the RabbitMQ consumer [{}] of [{}]", consumerTag, queue, e);
            }
        }
        try {
            channel.close();
        } catch (Exception e) {
            logger.error("Failed to close the RabbitMQ channel of [{}]", queue, e);
        }
        try {
            connection.close();
        } catch (Exception e) {
            logger.error("Failed to close the RabbitMQ connection of [{}]", queue, e);
        }
    }

    /**
     * Hands a message to the listener, and the failure of its handling to the listener's error callback
     * - whose own failure escapes to the channel.
     *
     * @param message the body of the message
     */
    private void deliver(String message) {
        try {
            listener.onMessage(message);
        } catch (RuntimeException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            listener.onError(e.getMessage());
        }
    }

    /**
     * Stop the consumer.
     */
    public void stop() {
        stopped.countDown();
    }

}
