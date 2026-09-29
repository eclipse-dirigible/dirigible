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

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * The {@code scheduler} health component (#7533): whether the scheduler can read its triggers from
 * the job store right now, how many there are, when one last fired and how many errors the
 * scheduler reported. DOWN when the triggers cannot be read or the scheduler is shut down - the
 * state in which the instance otherwise looks healthy while no scheduled job fires (#7049).
 */
@Component
class SchedulerHealthIndicator implements HealthIndicator {

    private final Scheduler scheduler;
    private final SchedulerActivity activity;

    SchedulerHealthIndicator(Scheduler scheduler, SchedulerActivity activity) {
        this.scheduler = scheduler;
        this.activity = activity;
    }

    @Override
    public Health health() {
        Health.Builder builder = readStore();
        activity.getLastFire()
                .ifPresent(fired -> builder.withDetail("lastFire", fired.toString()));
        builder.withDetail("errors", activity.getErrors());
        activity.getLastError()
                .ifPresent(message -> builder.withDetail("lastError", message));
        return builder.build();
    }

    private Health.Builder readStore() {
        try {
            int triggers = scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup())
                                    .size();
            boolean shutdown = scheduler.isShutdown();
            boolean started = scheduler.isStarted() && !scheduler.isInStandbyMode() && !shutdown;
            Health.Builder builder = shutdown ? Health.down() : Health.up();
            return builder.withDetail("started", started)
                          .withDetail("triggers", triggers);
        } catch (SchedulerException ex) {
            return Health.down(ex);
        }
    }
}
