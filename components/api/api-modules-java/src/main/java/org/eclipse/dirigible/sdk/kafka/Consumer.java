/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.kafka;

import org.eclipse.dirigible.components.api.kafka.KafkaFacade;

/**
 * Manages a long-lived Kafka consumer for a topic.
 * {@link #startListening(String, String, int, String)} begins consumption — the {@code handler}
 * argument is the registry path of a TS/JS handler (matching the platform listener convention) that
 * will be invoked for each record. {@code timeout} is the poll timeout in milliseconds.
 * <p>
 * Stop the consumer with {@link #stopListening(String, String)} when you're done, and call
 * {@link #closeProducer(String)} from a Java {@code @Component} that holds an ephemeral producer
 * (the consumer / producer share underlying configuration; {@code closeProducer} releases the
 * platform's cached producer for the given configuration).
 */
public final class Consumer {

    private Consumer() {}

    public static void startListening(String destination, String handler, int timeout, String configurationJson) {
        KafkaFacade.startListening(destination, handler, timeout, configurationJson);
    }

    public static void stopListening(String destination, String configurationJson) {
        KafkaFacade.stopListening(destination, configurationJson);
    }

    public static void closeProducer(String configurationJson) {
        KafkaFacade.closeProducer(configurationJson);
    }
}
