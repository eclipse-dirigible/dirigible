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
 * Publishes a single record to a Kafka topic. {@code configurationJson} is the Kafka producer
 * configuration as a JSON document — {@code bootstrap.servers}, {@code acks},
 * {@code key.serializer}, and so on. {@code null} uses the platform default configuration; pass an
 * explicit JSON when you need to override broker, partitioner, or compression on a per-call basis.
 * <p>
 * For high-throughput producers, prefer keeping a single configuration alive across calls so the
 * underlying Kafka {@code Producer} can pool connections; configuration changes between calls cause
 * the platform to rebuild the producer.
 */
public final class Producer {

    private Producer() {}

    public static void send(String destination, String key, String value, String configurationJson) {
        KafkaFacade.send(destination, key, value, configurationJson);
    }

    public static void send(String destination, String key, String value) {
        KafkaFacade.send(destination, key, value, null);
    }
}
