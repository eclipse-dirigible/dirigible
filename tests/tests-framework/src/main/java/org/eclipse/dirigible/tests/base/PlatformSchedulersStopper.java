/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import java.util.List;
import org.flowable.engine.ProcessEngine;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.stereotype.Component;

/**
 * Destroys the platform's schedulers before {@link DirigibleCleaner} drops the SystemDB schema they
 * run on.
 * <p>
 * Both of them keep polling that schema in the background - Quartz's cluster manager checks in on
 * {@code qrtz_scheduler_state}, Flowable's async executor acquires from {@code act_ru_job} - and
 * both issue a last round of statements when they are closed. The cleaner runs from a
 * {@code @PreDestroy}, i.e. while every other bean is still alive, so on PostgreSQL - where the
 * schema really is dropped instead of being discarded with the JVM, as the file-backed H2 SystemDB
 * is - each of those threads then logged an ERROR with a stack trace per poll: hundreds per CI
 * shard behind a green run, which is what made a PostgreSQL job log unreadable (#7285).
 * <p>
 * The beans are destroyed through the bean factory rather than shut down through their own APIs:
 * that runs Spring's own destruction for them exactly once, at a moment when their tables still
 * exist, and de-registers them, so the context close that follows does not close them a second time
 * against the dropped schema. Their dependents are destroyed with them, as during any context
 * close.
 */
@Component
class PlatformSchedulersStopper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformSchedulersStopper.class);

    /**
     * The process engine goes first - closing it unlocks the jobs its async executor holds, which is
     * work for the very scheduler that is stopped next.
     */
    private static final List<Class<?>> SYSTEM_DB_SCHEDULER_TYPES = List.of(ProcessEngine.class, Scheduler.class);

    private final ConfigurableListableBeanFactory beanFactory;

    PlatformSchedulersStopper(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    void stopSchedulers() {
        if (!(beanFactory instanceof DefaultSingletonBeanRegistry singletonRegistry)) {
            LOGGER.warn("Bean factory [{}] cannot destroy a single bean - the schedulers are left to the context close", beanFactory);
            return;
        }
        SYSTEM_DB_SCHEDULER_TYPES.forEach(type -> stopSchedulersOfType(type, singletonRegistry));
    }

    private void stopSchedulersOfType(Class<?> schedulerType, DefaultSingletonBeanRegistry singletonRegistry) {
        // singletons only, and without initialising anything that has not been created yet
        for (String beanName : beanFactory.getBeanNamesForType(schedulerType, false, false)) {
            LOGGER.info("Destroying [{}] of type [{}] before the schema it runs on is dropped...", beanName, schedulerType.getName());
            try {
                singletonRegistry.destroySingleton(beanName);
            } catch (RuntimeException ex) {
                LOGGER.warn("Failed to destroy [{}] of type [{}]", beanName, schedulerType.getName(), ex);
            }
        }
    }
}
