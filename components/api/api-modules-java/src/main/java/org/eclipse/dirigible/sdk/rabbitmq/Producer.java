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
 * Publishes a single message to a RabbitMQ queue. The message is delivered using the platform's
 * default channel configuration — for routing to topic / fanout exchanges, configure them through a
 * {@code .rabbitmq} artefact (see {@code engine-rabbitmq}) and publish to the resulting queue here.
 */
public final class Producer {

    private Producer() {}

    public static void send(String queue, String message) {
        RabbitMQFacade.send(queue, message);
    }
}
