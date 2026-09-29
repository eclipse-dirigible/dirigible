/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.config;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.stereotype.Component;

/**
 * What the scheduler has done since boot, for the {@code scheduler} health component (#7533): when
 * a trigger last fired - a fire is a trigger the scheduler read back from its job store - and how
 * many errors it reported, which is how a job store that fails while acquiring the next triggers
 * surfaces (the scheduler keeps polling and logs; nothing else notices).
 */
@Component
class SchedulerActivity {

    private final AtomicReference<Instant> lastFire = new AtomicReference<>();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    private final TriggerListener triggerListener = new TriggerListenerSupport() {

        @Override
        public String getName() {
            return SchedulerActivity.class.getName();
        }

        @Override
        public void triggerFired(Trigger trigger, JobExecutionContext context) {
            lastFire.set(Instant.now());
        }
    };

    private final SchedulerListener schedulerListener = new SchedulerListenerSupport() {

        @Override
        public void schedulerError(String message, SchedulerException cause) {
            errors.incrementAndGet();
            lastError.set(message);
        }
    };

    /** The listener recording the fires, registered with the scheduler. */
    TriggerListener triggerListener() {
        return triggerListener;
    }

    /** The listener recording the errors, registered with the scheduler. */
    SchedulerListener schedulerListener() {
        return schedulerListener;
    }

    /** When a trigger last fired, empty before the first fire. */
    Optional<Instant> getLastFire() {
        return Optional.ofNullable(lastFire.get());
    }

    /** The errors the scheduler reported since boot. */
    long getErrors() {
        return errors.get();
    }

    /** The message of the last reported error, empty when there was none. */
    Optional<String> getLastError() {
        return Optional.ofNullable(lastError.get());
    }
}
