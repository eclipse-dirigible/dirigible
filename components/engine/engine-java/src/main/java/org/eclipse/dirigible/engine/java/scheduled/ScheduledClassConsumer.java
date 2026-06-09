/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.scheduled;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.dirigible.sdk.job.Scheduled;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that registers client classes annotated with {@link Scheduled} as
 * cron-triggered tasks.
 *
 * <p>
 * The annotated class must expose a public no-arg {@code run()} method. The class is instantiated
 * once per load cycle; hot-reload cancels the old schedule and re-schedules with the updated class.
 *
 * <p>
 * A dedicated {@link ThreadPoolTaskScheduler} is created and owned by this consumer so that
 * {@code @Scheduled} support does not require {@code @EnableScheduling} in the host application.
 */
@Component
@Order(400)
public class ScheduledClassConsumer implements JavaClassConsumer, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledClassConsumer.class);

    private final TaskScheduler taskScheduler;

    /** fqn → active ScheduledFuture, for cancellation on unload. */
    private final ConcurrentMap<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public ScheduledClassConsumer() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("java-scheduled-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Scheduled.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Scheduled ann = info.type()
                            .getAnnotation(Scheduled.class);
        Method runMethod;
        try {
            runMethod = info.type()
                            .getMethod("run");
        } catch (NoSuchMethodException e) {
            LOGGER.error("@Scheduled class [{}] must expose a public no-arg run() method; skipped.", info.fqn());
            return;
        }

        Object instance;
        try {
            instance = info.type()
                           .getDeclaredConstructor()
                           .newInstance();
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to instantiate @Scheduled class [{}]: {}", info.fqn(), e.getMessage(), e);
            return;
        }

        cancelExisting(info.fqn());

        CronTrigger trigger;
        try {
            trigger = new CronTrigger(ann.expression());
        } catch (IllegalArgumentException e) {
            LOGGER.error("@Scheduled class [{}] has invalid cron expression '{}': {}", info.fqn(), ann.expression(), e.getMessage());
            return;
        }

        ScheduledFuture<?> future = taskScheduler.schedule(() -> invoke(runMethod, instance, info.fqn()), trigger);
        futures.put(info.fqn(), future);
        LOGGER.info("Scheduled Java class [{}] with cron '{}'.", info.fqn(), ann.expression());
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        cancelExisting(info.fqn());
        LOGGER.info("Unscheduled Java class [{}].", info.fqn());
    }

    @Override
    public void destroy() {
        futures.values()
               .forEach(f -> f.cancel(false));
        futures.clear();
        if (taskScheduler instanceof ThreadPoolTaskScheduler tpts) {
            tpts.shutdown();
        }
    }

    private void cancelExisting(String fqn) {
        ScheduledFuture<?> old = futures.remove(fqn);
        if (old != null) {
            old.cancel(false);
        }
    }

    private static void invoke(Method run, Object instance, String fqn) {
        try {
            run.invoke(instance);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("@Scheduled class [{}] run() threw: {}", fqn, e.getMessage(), e);
        }
    }
}
