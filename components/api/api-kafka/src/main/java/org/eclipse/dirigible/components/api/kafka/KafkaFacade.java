/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class KafkaFacade.
 */
@Component
public class KafkaFacade {

    /** The Constant DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER. */
    private static final String DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER = "DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER";

    /** The Constant DIRIGIBLE_KAFKA_ACKS. */
    private static final String DIRIGIBLE_KAFKA_ACKS = "DIRIGIBLE_KAFKA_ACKS";

    /** The Constant DIRIGIBLE_KAFKA_KEY_SERIALIZER. */
    private static final String DIRIGIBLE_KAFKA_KEY_SERIALIZER = "DIRIGIBLE_KAFKA_KEY_SERIALIZER";

    /** The Constant DIRIGIBLE_KAFKA_VALUE_SERIALIZER. */
    private static final String DIRIGIBLE_KAFKA_VALUE_SERIALIZER = "DIRIGIBLE_KAFKA_VALUE_SERIALIZER";

    /** The Constant DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED. */
    private static final String DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED = "DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED";

    /** The Constant DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL. */
    private static final String DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL = "DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL";


    /** The Constant DEFAULT_BOOTSTRAP_SERVER. */
    private static final String DEFAULT_BOOTSTRAP_SERVER = "localhost:9092";

    /** The Constant DIRIGIBLE_KAFKA_ACKS_ALL. */
    private static final String DIRIGIBLE_KAFKA_ACKS_ALL = "all";

    /** The Constant DIRIGIBLE_KAFKA_SERIALIZER_STRING. */
    private static final String DIRIGIBLE_KAFKA_SERIALIZER_STRING = "org.apache.kafka.common.serialization.StringSerializer";

    /** The Constant DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED_DEFAULT. */
    private static final String DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED_DEFAULT = "true";

    /** The Constant DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL_DEFAULT. */
    private static final String DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL_DEFAULT = "1000";


    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(KafkaFacade.class);

    /** The producers. */
    private static Map<String, Producer<String, String>> PRODUCERS = Collections.synchronizedMap(new HashMap());

    /** The consumers. */
    private static Map<String, KafkaConsumerRunner> CONSUMERS = Collections.synchronizedMap(new HashMap());

    /**
     * Send a key-value pair to a topic.
     *
     * @param destination the destination
     * @param key the key
     * @param value the value
     * @param configuration the configuration
     */
    public static final void send(String destination, String key, String value, String configuration) {
        if (configuration == null) {
            configuration = "{}";
        }

        Map map = GsonHelper.fromJson(configuration, Map.class);
        Producer<String, String> producer = null;

        String bootstrapServer = Configuration.get(DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER, DEFAULT_BOOTSTRAP_SERVER);
        String server = map.get("bootstrap.servers") != null ? map.get("bootstrap.servers")
                                                                  .toString()
                : bootstrapServer;
        if (server != null) {
            producer = PRODUCERS.get(server);
        }

        if (producer == null) {
            Properties props = new Properties();
            for (Object k : map.keySet()) {
                props.put(k, map.get(k));
            }
            if (props.get("bootstrap.servers") == null) {
                // default to localhost
                props.put("bootstrap.servers", bootstrapServer);
            }
            if (props.get("acks") == null) {
                // default to all
                String acks = Configuration.get(DIRIGIBLE_KAFKA_ACKS, DIRIGIBLE_KAFKA_ACKS_ALL);
                props.put("acks", acks);
            }
            if (props.get("key.serializer") == null) {
                // default to org.apache.kafka.common.serialization.StringSerializer
                String keySerializer = Configuration.get(DIRIGIBLE_KAFKA_KEY_SERIALIZER, DIRIGIBLE_KAFKA_SERIALIZER_STRING);
                props.put("key.serializer", keySerializer);
            }
            if (props.get("value.serializer") == null) {
                // default to org.apache.kafka.common.serialization.StringSerializer
                String valueSerializer = Configuration.get(DIRIGIBLE_KAFKA_VALUE_SERIALIZER, DIRIGIBLE_KAFKA_SERIALIZER_STRING);
                props.put("value.serializer", valueSerializer);
            }
            producer = new KafkaProducer<>(props);
            PRODUCERS.put(server, producer);
            if (logger.isInfoEnabled()) {
                logger.info("Kafka Producer [{}] created.", server);
            }
        }

        producer.send(new ProducerRecord<String, String>(destination, key, value));

        // producer.close();
    }

    /**
     * Close the producer per server endpoint.
     *
     * @param configuration the configuration
     */
    public static final void closeProducer(String configuration) {
        if (configuration == null) {
            configuration = "{}";
        }

        Map map = GsonHelper.fromJson(configuration, Map.class);
        Producer<String, String> producer = null;

        String bootstrapServer = Configuration.get(DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER, DEFAULT_BOOTSTRAP_SERVER);
        String server = map.get("bootstrap.servers") != null ? map.get("bootstrap.servers")
                                                                  .toString()
                : bootstrapServer;
        if (server != null) {
            producer = PRODUCERS.get(server);
        }

        if (producer != null) {
            producer.close();
            PRODUCERS.remove(server);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Kafka Producer [{}] has not been started yet.", server);
            }
        }
    }

    /**
     * Start listening.
     *
     * @param destination the destination
     * @param handler the handler
     * @param timeout the timeout
     * @param configuration the configuration
     */
    public static final void startListening(String destination, String handler, int timeout, String configuration) {
        if (configuration == null) {
            configuration = "{}";
        }

        Map map = GsonHelper.fromJson(configuration, Map.class);

        Consumer<String, String> consumer = null;
        KafkaConsumerRunner consumerRunner = null;

        String location = null;
        String bootstrapServer = Configuration.get(DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER, DEFAULT_BOOTSTRAP_SERVER);
        String server = map.get("bootstrap.servers") != null ? map.get("bootstrap.servers")
                                                                  .toString()
                : bootstrapServer;
        location = createLocation(destination, server);
        consumerRunner = CONSUMERS.get(location);

        if (consumerRunner == null) {
            Properties props = new Properties();
            for (Object k : map.keySet()) {
                props.put(k, map.get(k));
            }
            if (props.get("bootstrap.servers") == null) {
                // default to localhost
                props.put("bootstrap.servers", bootstrapServer);
            }
            if (props.get("group.id") == null) {
                // default to handler
                props.put("group.id", handler != null ? handler : destination);
            }
            if (props.get("enable.auto.commit") == null) {
                // autocommit
                String enableAutoCommit = Configuration.get(DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED, DIRIGIBLE_KAFKA_AUTOCOMMIT_ENABLED_DEFAULT);
                props.put("enable.auto.commit", enableAutoCommit);
            }
            if (props.get("auto.commit.interval.ms") == null) {
                // autocommit interval 1000
                String autoCommitInterval =
                        Configuration.get(DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL, DIRIGIBLE_KAFKA_AUTOCOMMIT_INTERVAL_DEFAULT);
                props.put("auto.commit.interval.ms", autoCommitInterval);
            }
            if (props.get("key.deserializer") == null) {
                // default to org.apache.kafka.common.serialization.StringSerializer
                String keySerializer = Configuration.get(DIRIGIBLE_KAFKA_KEY_SERIALIZER, DIRIGIBLE_KAFKA_SERIALIZER_STRING);
                props.put("key.deserializer", keySerializer);
            }
            if (props.get("value.deserializer") == null) {
                // default to org.apache.kafka.common.serialization.StringSerializer
                String valueSerializer = Configuration.get(DIRIGIBLE_KAFKA_VALUE_SERIALIZER, DIRIGIBLE_KAFKA_SERIALIZER_STRING);
                props.put("value.deserializer", valueSerializer);
            }

            consumer = new KafkaConsumer<>(props);

            consumerRunner = new KafkaConsumerRunner(consumer, destination, handler, timeout);
            Thread consumerThread = new Thread(consumerRunner);
            consumerThread.setDaemon(false);
            consumerThread.start();
            CONSUMERS.put(location, consumerRunner);
            if (logger.isInfoEnabled()) {
                logger.info("Kafka Consumer [{}] created.", location);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Kafka Consumer [{}] has already been started.", location);
            }
        }
    }

    /**
     * Stop listening.
     *
     * @param destination the destination
     * @param configuration the configuration
     */
    public static final void stopListening(String destination, String configuration) {
        if (configuration == null) {
            configuration = "{}";
        }

        Map map = GsonHelper.fromJson(configuration, Map.class);

        KafkaConsumerRunner consumerRunner = null;

        String location = null;
        String bootstrapServer = Configuration.get(DIRIGIBLE_KAFKA_BOOTSTRAP_SERVER, DEFAULT_BOOTSTRAP_SERVER);
        String server = map.get("bootstrap.servers") != null ? map.get("bootstrap.servers")
                                                                  .toString()
                : bootstrapServer;
        location = createLocation(destination, server);
        consumerRunner = CONSUMERS.get(location);

        if (consumerRunner != null) {
            consumerRunner.stop();
            CONSUMERS.remove(location);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Kafka Consumer [" + location + "] has not been started yet.");
            }
        }
    }

    /**
     * Create internal identifier for a consumer.
     *
     * @param destination the destination
     * @param server the server
     * @return the identifier
     */
    private static String createLocation(String destination, String server) {
        return "[" + server + "]:[" + destination + "]";
    }

}
