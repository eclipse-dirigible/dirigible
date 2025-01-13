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

import static java.text.MessageFormat.format;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Class KafkaConsumerRunner.
 */
public class KafkaConsumerRunner implements Runnable {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerRunner.class);

    /** The Constant DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE. */
    private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE = "messaging/wrappers/onMessage";

    /** The Constant DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR. */
    private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR = "messaging/wrappers/onError";

    /** The stopped. */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /** The consumer. */
    private final Consumer consumer;

    /** The name. */
    private final String name;

    /** The handler. */
    private final String handler;

    /** The timeout. */
    private int timeout = 1000;

    @Autowired
    private JavascriptService javascriptService;

    /**
     * Instantiates a new kafka consumer runner.
     *
     * @param consumer the consumer
     * @param name the name
     * @param handler the handler
     * @param timeout the timeout
     */
    public KafkaConsumerRunner(Consumer consumer, String name, String handler, int timeout) {
        this.consumer = consumer;
        this.name = name;
        this.handler = handler;
        this.timeout = timeout;
    }

    /**
     * Start the consumer.
     */
    @Override
    public void run() {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Starting a Kafka listener for {} ...", this.name);
            }
            consumer.subscribe(Arrays.asList(this.name));
            while (!stopped.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(this.timeout));
                for (ConsumerRecord<String, String> record : records) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(format("Start processing a received record in [{0}] by [{1}] ...", this.name, this.handler));
                    }
                    if (this.handler != null) {
                        Map<Object, Object> context = createMessagingContext();
                        context.put("message", escapeCodeString(GsonHelper.toJson(record)));
                        try {
                            RepositoryPath path = new RepositoryPath(DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE);
                            JavascriptService.get()
                                             .handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
                        } catch (Exception e) {
                            if (logger.isErrorEnabled()) {
                                logger.error(e.getMessage(), e);
                            }
                            try {
                                context.put("error", escapeCodeString(e.getMessage()));
                                RepositoryPath path = new RepositoryPath(DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR);
                                javascriptService.handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
                            } catch (Exception es) {
                                if (logger.isErrorEnabled()) {
                                    logger.error(es.getMessage(), es);
                                }
                            }
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("[Kafka Consumer] %s -  offset = %d, key = %s, value = %s%n", this.name,
                                    record.offset(), record.key(), record.value()));
                        }
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace(format("Done processing the received record in [{0}] by [{1}]", this.name, this.handler));
                    }
                }

            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!stopped.get())
                throw e;
        } finally {
            consumer.close();
        }
    }

    /**
     * Stop the consumer.
     */
    public void stop() {
        stopped.set(true);
        consumer.wakeup();
    }

    /**
     * Create a context map and set the handler.
     *
     * @return the context map
     */
    private Map<Object, Object> createMessagingContext() {
        Map<Object, Object> context = new HashMap<Object, Object>();
        context.put("handler", this.handler);
        return context;
    }

    /**
     * Escape code string.
     *
     * @param raw the raw
     * @return the string
     */
    private String escapeCodeString(String raw) {
        return raw.replace("'", "&amp;");
    }

}
