/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.rabbitmq;

import org.eclipse.dirigible.components.api.rabbitmq.RabbitMQFacade;

/**
 * Subscribes a handler to a RabbitMQ queue. The {@code handler} string is the registry path of a
 * TS/JS script (matching the platform listener convention); the platform spawns a consumer thread,
 * deserialises message bodies as strings, and invokes the handler per message.
 * <p>
 * For per-message ack control or for message inspection beyond the body, drop down to the bare
 * RabbitMQ client through a {@code @Component} bean and a {@code .rabbitmq} configuration.
 */
public final class Consumer {

    private Consumer() {}

    public static void startListening(String queue, String handler) {
        RabbitMQFacade.startListening(queue, handler);
    }

    public static void stopListening(String queue, String handler) {
        RabbitMQFacade.stopListening(queue, handler);
    }
}
