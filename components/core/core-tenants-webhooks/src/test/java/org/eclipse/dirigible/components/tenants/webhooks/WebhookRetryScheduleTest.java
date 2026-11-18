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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.Test;

/**
 * The retry schedule: growing delays whose last one repeats, given up after the window.
 */
class WebhookRetryScheduleTest {

    private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");

    @Test
    void theDefaultGrowsFromAMinuteToSixHoursAndRetriesForADay() {
        WebhookRetrySchedule schedule = WebhookRetrySchedule.parse(DirigibleConfig.TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS.getDefaultValue(),
                Long.parseLong(DirigibleConfig.TENANT_WEBHOOKS_RETRY_WINDOW_SECONDS.getDefaultValue()));
        assertEquals(NOW.plus(Duration.ofMinutes(1)), schedule.nextAttemptAt(1, NOW));
        assertEquals(NOW.plus(Duration.ofMinutes(5)), schedule.nextAttemptAt(2, NOW));
        assertEquals(NOW.plus(Duration.ofMinutes(30)), schedule.nextAttemptAt(3, NOW));
        assertEquals(NOW.plus(Duration.ofHours(2)), schedule.nextAttemptAt(4, NOW));
        assertEquals(NOW.plus(Duration.ofHours(6)), schedule.nextAttemptAt(5, NOW));
        assertEquals(NOW.plus(Duration.ofHours(6)), schedule.nextAttemptAt(12, NOW), "the last delay repeats");
        assertEquals(Duration.ofHours(24), schedule.window());
    }

    @Test
    void givesUpOnlyOnceAFailureComesAfterTheWindow() {
        WebhookRetrySchedule schedule = WebhookRetrySchedule.parse("60", 86400);
        assertFalse(schedule.givesUp(NOW, NOW.plus(Duration.ofHours(23))));
        assertTrue(schedule.givesUp(NOW, NOW.plus(Duration.ofHours(24))));
        assertTrue(schedule.givesUp(NOW, NOW.plus(Duration.ofDays(3))));
    }

    @Test
    void skipsWhatIsNotAPositiveNumberAndFallsBackToAMinute() {
        assertEquals(List.of(Duration.ofSeconds(5), Duration.ofSeconds(10)), WebhookRetrySchedule.parse(" 5, x, -1, 0, 10 ", 60)
                                                                                                 .delays());
        assertEquals(List.of(Duration.ofMinutes(1)), WebhookRetrySchedule.parse("nonsense", 60)
                                                                         .delays());
        assertEquals(List.of(Duration.ofMinutes(1)), WebhookRetrySchedule.parse(null, 60)
                                                                         .delays());
    }
}
