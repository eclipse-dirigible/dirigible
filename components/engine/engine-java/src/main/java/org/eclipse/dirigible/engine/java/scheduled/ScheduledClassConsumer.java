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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.job.JobHandler;
import org.eclipse.dirigible.sdk.job.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * {@link JavaClassConsumer} that schedules client {@link Scheduled @Scheduled} tasks on a cron
 * trigger. Two styles are supported:
 * <ul>
 * <li><b>class level</b> — a {@code @Scheduled} class that implements {@link JobHandler} (direct
 * dispatch) or exposes a public no-arg {@code run()} (reflective fallback);</li>
 * <li><b>method level</b> — public no-arg methods annotated {@code @Scheduled} on any client bean
 * ({@code @Component} / {@code @Controller} / …), Spring's {@code @Scheduled}-on-a-method
 * style.</li>
 * </ul>
 * The bean instance is built (with constructor + field injection) by the
 * {@link ComponentContainer}; this consumer only fetches it and wires the cron schedule. Hot-reload
 * cancels the old schedule(s) and re-registers from the updated class.
 *
 * <p>
 * A dedicated {@link ThreadPoolTaskScheduler} is owned by this consumer so {@code @Scheduled}
 * support does not require {@code @EnableScheduling} in the host application.
 */
@Component
@Order(400)
public class ScheduledClassConsumer implements JavaClassConsumer, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledClassConsumer.class);

    private final ComponentContainer componentContainer;

    private final TaskScheduler taskScheduler;

    /** fqn → active ScheduledFutures (one class may register several method-level schedules). */
    private final ConcurrentMap<String, List<ScheduledFuture<?>>> futures = new ConcurrentHashMap<>();

    @Autowired
    public ScheduledClassConsumer(ComponentContainer componentContainer) {
        this.componentContainer = componentContainer;
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("java-scheduled-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Scheduled.class) || hasScheduledMethod(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("@Scheduled [{}] was not instantiated as a bean (a method-level @Scheduled must live on a @Component); skipped.",
                    info.fqn());
            return;
        }

        cancelExisting(info.fqn());
        List<ScheduledFuture<?>> scheduled = new ArrayList<>();

        Scheduled classLevel = type.getAnnotation(Scheduled.class);
        if (classLevel != null) {
            Runnable task = classLevelTask(instance, info.fqn());
            if (task != null) {
                schedule(scheduled, task, classLevel.expression(), info.fqn(), info.fqn());
            }
        }

        for (Method method : type.getDeclaredMethods()) {
            Scheduled methodLevel = method.getAnnotation(Scheduled.class);
            if (methodLevel == null) {
                continue;
            }
            if (!isEligibleMethod(method)) {
                LOGGER.error("@Scheduled method [{}#{}] must be public and take no parameters; skipped.", info.fqn(), method.getName());
                continue;
            }
            method.setAccessible(true);
            String label = info.fqn() + "#" + method.getName();
            schedule(scheduled, () -> invoke(method, instance, label), methodLevel.expression(), label, info.fqn());
        }

        if (scheduled.isEmpty()) {
            LOGGER.warn("@Scheduled [{}] produced no schedule — a class-level @Scheduled needs JobHandler or a run() method, "
                    + "a method-level one needs a public no-arg @Scheduled method.", info.fqn());
            return;
        }
        futures.put(info.fqn(), scheduled);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        cancelExisting(info.fqn());
        LOGGER.info("Unscheduled Java class [{}].", info.fqn());
    }

    @Override
    public void destroy() {
        futures.values()
               .forEach(list -> list.forEach(f -> f.cancel(false)));
        futures.clear();
        if (taskScheduler instanceof ThreadPoolTaskScheduler tpts) {
            tpts.shutdown();
        }
    }

    private Runnable classLevelTask(Object instance, String fqn) {
        if (instance instanceof JobHandler typed) {
            return () -> {
                try {
                    typed.run();
                } catch (RuntimeException ex) {
                    LOGGER.error("@Scheduled class [{}] run() threw: {}", fqn, ex.getMessage(), ex);
                }
            };
        }
        Method runMethod;
        try {
            runMethod = instance.getClass()
                                .getMethod("run");
        } catch (NoSuchMethodException e) {
            // Only an error if there are no method-level @Scheduled either; the caller logs that case.
            return null;
        }
        return () -> invoke(runMethod, instance, fqn);
    }

    private void schedule(List<ScheduledFuture<?>> sink, Runnable task, String expression, String label, String fqn) {
        CronTrigger trigger;
        try {
            trigger = new CronTrigger(expression);
        } catch (IllegalArgumentException e) {
            LOGGER.error("@Scheduled [{}] has invalid cron expression '{}': {}", label, expression, e.getMessage(), e);
            return;
        }
        sink.add(taskScheduler.schedule(task, trigger));
        LOGGER.info("Scheduled Java task [{}] with cron '{}'.", label, expression);
    }

    private void cancelExisting(String fqn) {
        List<ScheduledFuture<?>> old = futures.remove(fqn);
        if (old != null) {
            old.forEach(f -> f.cancel(false));
        }
    }

    private static boolean hasScheduledMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Scheduled.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0 && !method.isSynthetic();
    }

    private static void invoke(Method method, Object instance, String label) {
        try {
            method.invoke(instance);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LOGGER.error("@Scheduled [{}] threw: {}", label, cause.getMessage(), cause);
        } catch (RuntimeException e) {
            LOGGER.error("@Scheduled [{}] threw: {}", label, e.getMessage(), e);
        }
    }
}
