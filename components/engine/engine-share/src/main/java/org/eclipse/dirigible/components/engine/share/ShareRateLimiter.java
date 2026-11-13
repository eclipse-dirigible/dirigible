/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.share;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.stereotype.Component;

/**
 * Bounds what one remote address, and one link, may ask the public share route for (issue #8613).
 *
 * <p>
 * Three fixed windows, counted in memory: requests per address and requests per link, each over a
 * minute, and the MISSES of an address - unknown, expired and revoked links - over ten minutes. An
 * address past its miss limit is refused for the rest of that window, which is what makes guessing
 * tokens slow rather than merely pointless. The per-link window is keyed by the hash of whatever
 * was presented, existing link or not, so a refusal says nothing about whether the link exists.
 *
 * <p>
 * The counters are per node. That is deliberate: they are a throttle, not a decision about a link -
 * whether a link answers is read from the database on every request, so a revoke needs no counter
 * to agree with it. Behind N nodes the limits are N times looser, never unbounded.
 */
@Component
class ShareRateLimiter {

    static final Duration REQUEST_WINDOW = Duration.ofMinutes(1);

    static final Duration MISS_WINDOW = Duration.ofMinutes(10);

    /** Past this many counters the expired ones are dropped, so the map cannot grow without bound. */
    private static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private final LongSupplier clock;

    private final int requestLimit;

    private final int missLimit;

    ShareRateLimiter() {
        this(System::currentTimeMillis, DirigibleConfig.DOCUMENTS_SHARE_RATE_LIMIT_PER_MINUTE.getIntValue(),
                DirigibleConfig.DOCUMENTS_SHARE_MISS_LIMIT.getIntValue());
    }

    ShareRateLimiter(LongSupplier clock, int requestLimit, int missLimit) {
        this.clock = clock;
        this.requestLimit = requestLimit;
        this.missLimit = missLimit;
    }

    /**
     * Counts a request and says whether it may be answered.
     *
     * @param address the remote address
     * @param tokenHash the hash of the presented token
     * @return the seconds the caller is asked to wait, {@code 0} when the request may be answered
     */
    long acquire(String address, String tokenHash) {
        long now = clock.getAsLong();
        purgeWhenLarge(now);
        long missWait = remaining("miss:" + address, missLimit, now);
        if (missWait > 0) {
            return missWait;
        }
        long addressWait = count("address:" + address, REQUEST_WINDOW, requestLimit, now);
        long tokenWait = count("token:" + tokenHash, REQUEST_WINDOW, requestLimit, now);
        return Math.max(addressWait, tokenWait);
    }

    /**
     * Records that an address asked for a link that does not answer.
     *
     * @param address the remote address
     */
    void miss(String address) {
        count("miss:" + address, MISS_WINDOW, Integer.MAX_VALUE, clock.getAsLong());
    }

    /** Counts one hit in a window and answers the wait once the window is over its limit. */
    private long count(String key, Duration length, int limit, long now) {
        Window window =
                windows.compute(key, (k, current) -> current == null || current.endsAt <= now ? new Window(now + length.toMillis(), 1)
                        : new Window(current.endsAt, current.count + 1));
        return window.count > limit ? secondsUntil(window.endsAt, now) : 0;
    }

    /** The wait of a window that already reached its limit, without counting a hit. */
    private long remaining(String key, int limit, long now) {
        Window window = windows.get(key);
        return window != null && window.endsAt > now && window.count >= limit ? secondsUntil(window.endsAt, now) : 0;
    }

    private void purgeWhenLarge(long now) {
        if (windows.size() > PURGE_THRESHOLD) {
            windows.values()
                   .removeIf(window -> window.endsAt <= now);
        }
    }

    private static long secondsUntil(long endsAt, long now) {
        return Math.max(1, (endsAt - now + 999) / 1000);
    }

    private record Window(long endsAt, int count) {
    }
}
