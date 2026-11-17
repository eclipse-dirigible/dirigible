/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When an undelivered webhook event is attempted again, and when it is given up.
 *
 * <p>
 * The delays grow by the configured list
 * ({@link DirigibleConfig#TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS}, by default 1 min, 5 min, 30 min,
 * 2 h, then every 6 h) and the event is given up once a failed attempt comes at least
 * {@link DirigibleConfig#TENANT_WEBHOOKS_RETRY_WINDOW_SECONDS} (24 h) after the event was recorded.
 *
 * @param delays the delays, the last one repeating; never empty
 * @param window how long an event is retried
 */
record WebhookRetrySchedule(List<Duration> delays, Duration window) {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookRetrySchedule.class);

    /** Used when the configured list holds no usable delay. */
    private static final Duration FALLBACK_DELAY = Duration.ofMinutes(1);

    /**
     * Instantiates a schedule.
     *
     * @param delays the delays, the last one repeating; never empty
     * @param window how long an event is retried
     */
    WebhookRetrySchedule {
        if (delays.isEmpty()) {
            throw new IllegalArgumentException("A retry schedule needs at least one delay");
        }
        delays = List.copyOf(delays);
    }

    /**
     * The configured schedule, read now.
     *
     * @return the schedule
     */
    static WebhookRetrySchedule configured() {
        return parse(DirigibleConfig.TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS.getStringValue(),
                DirigibleConfig.TENANT_WEBHOOKS_RETRY_WINDOW_SECONDS.getIntValue());
    }

    /**
     * Parses a schedule. A value that is not a positive number of seconds is skipped.
     *
     * @param delaysInSeconds the comma-separated delays
     * @param windowInSeconds the window
     * @return the schedule
     */
    static WebhookRetrySchedule parse(String delaysInSeconds, long windowInSeconds) {
        List<Duration> delays = new ArrayList<>();
        if (delaysInSeconds != null) {
            for (String part : delaysInSeconds.split(",")) {
                try {
                    long seconds = Long.parseLong(part.trim());
                    if (seconds > 0) {
                        delays.add(Duration.ofSeconds(seconds));
                    }
                } catch (NumberFormatException notANumber) {
                    // skipped, as documented: a typo in one delay must not stop every delivery
                    LOGGER.warn("Ignoring the webhook retry delay [{}] of [{}]: not a number of seconds", part,
                            DirigibleConfig.TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS.getKey(), notANumber);
                }
            }
        }
        if (delays.isEmpty()) {
            delays.add(FALLBACK_DELAY);
        }
        return new WebhookRetrySchedule(delays, Duration.ofSeconds(Math.max(0, windowInSeconds)));
    }

    /**
     * When the attempt after a failed one is due.
     *
     * @param failedAttempts how many attempts have failed so far, at least 1
     * @param now the moment of the last failure
     * @return the next attempt's moment
     */
    Instant nextAttemptAt(int failedAttempts, Instant now) {
        int index = Math.min(Math.max(failedAttempts, 1), delays.size()) - 1;
        return now.plus(delays.get(index));
    }

    /**
     * Whether a failed attempt at the given moment ends the event's retries.
     *
     * @param recordedAt when the event was recorded
     * @param now the moment of the failed attempt
     * @return true once the window is over
     */
    boolean givesUp(Instant recordedAt, Instant now) {
        return !now.isBefore(recordedAt.plus(window));
    }
}
