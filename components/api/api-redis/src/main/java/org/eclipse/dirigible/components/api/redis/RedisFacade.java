/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.redis;

import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

/**
 * The Class RedisFacade.
 */
@Component
public class RedisFacade {

    /**
     * The Constant DIRIGIBLE_REDIS_CLIENT_URI.
     */
    private static final String DIRIGIBLE_REDIS_CLIENT_URI = "DIRIGIBLE_REDIS_CLIENT_URI";

    /**
     * The Constant CLIENT_URI.
     */
    private static final String CLIENT_URI = "localhost:6379";

    /**
     * Gets the client.
     *
     * @return the client
     */
    public static Jedis getClient() {

        String[] splitUri = Configuration.get(DIRIGIBLE_REDIS_CLIENT_URI, CLIENT_URI)
                                         .split(":");

        String host = splitUri[0];
        int port = Integer.parseInt(splitUri[1]);

        return new Jedis(host, port);
    }
}
