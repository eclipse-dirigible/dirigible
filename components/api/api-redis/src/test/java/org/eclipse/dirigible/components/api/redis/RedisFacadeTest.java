/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.redis;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisFacadeTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

    @BeforeEach
    public void setUp() {
        redis.start();

        String host = redis.getHost();
        Integer port = redis.getFirstMappedPort();

        Configuration.set("DIRIGIBLE_REDIS_CLIENT_URI", host + ":" + port);
    }

    @Test
    public void getClient() {
        Jedis client = RedisFacade.getClient();
        client.set("key", "value");
        assertEquals("value", client.get("key"));
    }
}
