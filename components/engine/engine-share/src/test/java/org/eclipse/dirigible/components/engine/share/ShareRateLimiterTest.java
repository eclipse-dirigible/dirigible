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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** The throttle of the public share route (dirigible #8613). */
class ShareRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);

    private final ShareRateLimiter limiter = new ShareRateLimiter(now::get, 3, 2);

    @Test
    void anAddressIsAnsweredUpToItsLimitAndThenAskedToWait() {
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.acquire("10.0.0.1", "hash-" + i));
        }
        long wait = limiter.acquire("10.0.0.1", "hash-3");
        assertTrue(wait > 0 && wait <= 60, "the wait is what is left of the minute, was " + wait);
        // Another address is not affected by it.
        assertEquals(0, limiter.acquire("10.0.0.2", "hash-9"));
    }

    @Test
    void aLinkIsThrottledWhoeverAsksForIt() {
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.acquire("10.0.1." + i, "one-link"));
        }
        assertTrue(limiter.acquire("10.0.1.9", "one-link") > 0);
    }

    @Test
    void theWindowEndsAndTheAddressIsAnsweredAgain() {
        for (int i = 0; i < 4; i++) {
            limiter.acquire("10.0.0.1", "hash-" + i);
        }
        now.addAndGet(ShareRateLimiter.REQUEST_WINDOW.toMillis());
        assertEquals(0, limiter.acquire("10.0.0.1", "hash-5"));
    }

    @Test
    void anAddressThatKeepsMissingIsRefusedForTheRestOfTheMissWindow() {
        limiter.miss("10.0.0.7");
        assertEquals(0, limiter.acquire("10.0.0.7", "guess-1"));
        limiter.miss("10.0.0.7");
        // Two misses is the limit: a third guess is not even looked up, and a minute does not lift it.
        assertTrue(limiter.acquire("10.0.0.7", "guess-2") > 0);
        now.addAndGet(ShareRateLimiter.REQUEST_WINDOW.toMillis());
        assertTrue(limiter.acquire("10.0.0.7", "guess-3") > 60);
        now.addAndGet(ShareRateLimiter.MISS_WINDOW.toMillis());
        assertEquals(0, limiter.acquire("10.0.0.7", "guess-4"));
    }
}
