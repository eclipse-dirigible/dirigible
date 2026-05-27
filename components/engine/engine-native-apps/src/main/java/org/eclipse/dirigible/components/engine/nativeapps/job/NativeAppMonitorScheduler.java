/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.job;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers the {@link NativeAppMonitorJob} with the platform Quartz {@link Scheduler} once the
 * Spring context is ready. Cron is configurable via {@code DIRIGIBLE_NATIVE_APP_MONITOR_CRON}.
 */
@Component
public class NativeAppMonitorScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppMonitorScheduler.class);

    private static final String JOB_GROUP = "dirigible-native-app";
    private static final String JOB_NAME = "native-app-monitor";

    private final Scheduler scheduler;

    public NativeAppMonitorScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleMonitor() {
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            TriggerKey triggerKey = TriggerKey.triggerKey(JOB_NAME, JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                return;
            }
            String cron = DirigibleConfig.NATIVE_APP_MONITOR_CRON.getStringValue();
            JobDetail job = JobBuilder.newJob(NativeAppMonitorJob.class)
                                      .withIdentity(jobKey)
                                      .storeDurably(false)
                                      .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                                            .withIdentity(triggerKey)
                                            .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                                            .build();
            scheduler.scheduleJob(job, trigger);
            LOGGER.info("Native-app monitor scheduled with cron [{}].", cron);
        } catch (SchedulerException ex) {
            LOGGER.error("Failed to schedule the native-app monitor: {}", ex.getMessage(), ex);
        }
    }
}
