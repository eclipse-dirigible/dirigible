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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * The Class RabbitMQFacade.
 */
@Component
public class RabbitMQFacade extends Thread {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQFacade.class);

    /**
     * The Constant DIRIGIBLE_RABBITMQ_USERNAME.
     */
    private static final String DIRIGIBLE_RABBITMQ_USERNAME = "guest";

    /**
     * The Constant DIRIGIBLE_RABBITMQ_PASSWORD.
     */
    private static final String DIRIGIBLE_RABBITMQ_PASSWORD = "guest";

    /**
     * The Constant RABBITMQ_CLIENT.
     */
    private static final String RABBITMQ_CLIENT = "127.0.0.1:5672";

    /**
     * The Constant DIRIGIBLE_RABBITMQ_CLIENT_URI.
     */
    private static final String DIRIGIBLE_RABBITMQ_CLIENT_URI = "DIRIGIBLE_RABBITMQ_CLIENT_URI";

    /**
     * The consumers.
     */
    private static final Map<String, RabbitMQReceiverRunner> CONSUMERS = Collections.synchronizedMap(new HashMap<>());

    /**
     * Send message to given queue.
     *
     * @param queue the queue being used
     * @param message the message to be delivered
     */
    public static void send(String queue, String message) {
        try {
            Connection connection = connect();
            Channel channel = createChannel(connection);

            channel.queueDeclare(queue, false, false, false, null);
            channel.basicPublish("", queue, null, message.getBytes());
            if (logger.isInfoEnabled()) {
                logger.info("Sent: " + "'" + message + "'" + " to [" + queue + "]");
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error sending message: " + e.toString(), e);
            }
        }
    }

    /**
     * Start listening given queue and destination.
     *
     * @param queue the queue being used
     * @param handler the destination for the message
     */
    public static final void startListening(String queue, String handler) {
        Connection connection = connect();
        Channel channel = createChannel(connection);
        String location = null;

        RabbitMQReceiverRunner receiverRunner = null;
        location = createLocation(queue, handler);
        receiverRunner = CONSUMERS.get(location);

        if (receiverRunner == null) {
            receiverRunner = new RabbitMQReceiverRunner(connection, channel, queue, handler);
            Thread receiverThread = new Thread(receiverRunner);
            receiverThread.setDaemon(false);
            receiverThread.start();

            CONSUMERS.put(location, receiverRunner);
            if (logger.isInfoEnabled()) {
                logger.info("RabbitMQ receiver created for [" + queue + "]");
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("RabbitMQ receiver [" + queue + "] has already been started.");
            }
        }
    }

    /**
     * Stop listening on given queue and destination.
     *
     * @param queue the queue being used
     * @param handler the destination for the message
     */
    public static final void stopListening(String queue, String handler) {
        RabbitMQReceiverRunner receiverRunner = null;
        String location = null;

        location = createLocation(queue, handler);
        receiverRunner = CONSUMERS.get(location);
        if (receiverRunner != null) {
            receiverRunner.stop();
            CONSUMERS.remove(location);
            if (logger.isInfoEnabled()) {
                logger.info("RabbitMQ receiver stopped for [" + queue + "]");
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("RabbitMQ receiver [" + queue + "] has not been started yet.");
            }
        }
    }

    /**
     * Connect.
     *
     * @return the connection
     */
    private static Connection connect() {
        String[] splitUri = Configuration.get(DIRIGIBLE_RABBITMQ_CLIENT_URI, RABBITMQ_CLIENT)
                                         .split(":");
        String host = splitUri[0];
        int port = Integer.parseInt(splitUri[1]);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(DIRIGIBLE_RABBITMQ_USERNAME);
        factory.setPassword(DIRIGIBLE_RABBITMQ_PASSWORD);
        factory.setPort(port);
        factory.setConnectionTimeout(20000);
        Connection connection = null;

        try {
            connection = factory.newConnection();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error establishing connection to AMQP: " + e.toString(), e);
            }
        }
        return connection;
    }

    /**
     * Creates the channel.
     *
     * @param connection the connection
     * @return the channel
     */
    private static Channel createChannel(Connection connection) {
        Channel channel = null;
        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error creating channel: " + e.toString(), e);
            }
        }
        return channel;
    }

    /**
     * Create internal identifier for a consumer.
     *
     * @param queue the queue being used
     * @param handler the destination for the message
     * @return the identifier
     */
    private static String createLocation(String queue, String handler) {
        return "[" + queue + "]:[" + handler + "]";
    }
}
