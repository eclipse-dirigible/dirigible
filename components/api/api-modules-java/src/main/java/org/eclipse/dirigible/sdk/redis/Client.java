/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.redis;

import org.eclipse.dirigible.components.api.redis.RedisFacade;
import redis.clients.jedis.Jedis;

/**
 * Hands back a connected {@link Jedis} client wired against the platform's Redis configuration
 * ({@code DIRIGIBLE_REDIS_*}). Jedis exposes the full Redis command surface — strings, lists, sets,
 * sorted sets, hashes, streams, pub/sub — so the SDK does not duplicate it; the wrapper exists
 * purely to lift configuration boilerplate.
 * <p>
 * Treat the returned client as a per-call resource — close it (or use try-with-resources) so the
 * underlying connection returns to the platform-managed pool. Sharing a single Jedis instance
 * across threads is not safe.
 */
public final class Client {

    private Client() {}

    public static Jedis getClient() {
        return RedisFacade.getClient();
    }
}
