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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.function.Supplier;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * The stopper exists so that the Quartz scheduler and the Flowable process engine are torn down
 * while the SystemDB schema they run on still exists - and torn down exactly once, so the context
 * close that follows the schema drop does not talk to the dropped schema again (#7285).
 */
class PlatformSchedulersStopperTest {

    private static final String SCHEDULER_BEAN = "scheduler";
    private static final String PROCESS_ENGINE_BEAN = "getProcessEngine";
    private static final String UNRELATED_BEAN = "unrelated";

    private DefaultListableBeanFactory beanFactory;
    private PlatformSchedulersStopper stopper;

    private Scheduler scheduler;
    private ProcessEngine processEngine;
    private AutoCloseable unrelated;

    @BeforeEach
    void setUp() {
        beanFactory = new DefaultListableBeanFactory();
        stopper = new PlatformSchedulersStopper(beanFactory);

        scheduler = registerSingleton(SCHEDULER_BEAN, Scheduler.class, "shutdown");
        processEngine = registerSingleton(PROCESS_ENGINE_BEAN, ProcessEngine.class, "close");
        unrelated = registerSingleton(UNRELATED_BEAN, AutoCloseable.class, "close");
    }

    @Test
    void shouldDestroyTheSchedulersWithoutTouchingTheRestOfTheContext() throws Exception {
        stopper.stopSchedulers();

        verify(scheduler).shutdown();
        verify(processEngine).close();
        verifyNoInteractions(unrelated);
    }

    @Test
    void shouldNotLeaveTheSchedulersToBeDestroyedAgainByTheContextClose() throws Exception {
        stopper.stopSchedulers();

        beanFactory.destroySingletons();

        verify(scheduler, times(1)).shutdown();
        verify(processEngine, times(1)).close();
        verify(unrelated).close();
    }

    @Test
    void shouldBeSilentWhenTheSchedulersWereNeverCreated() {
        DefaultListableBeanFactory emptyBeanFactory = new DefaultListableBeanFactory();

        new PlatformSchedulersStopper(emptyBeanFactory).stopSchedulers();
    }

    private <T> T registerSingleton(String beanName, Class<T> beanType, String destroyMethodName) {
        T bean = mock(beanType);
        Supplier<T> instanceSupplier = () -> bean;

        AbstractBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
        beanDefinition.setInstanceSupplier(instanceSupplier);
        beanDefinition.setDestroyMethodName(destroyMethodName);
        beanFactory.registerBeanDefinition(beanName, beanDefinition);

        // create it, so that it is a singleton the context close would destroy
        beanFactory.getBean(beanName, beanType);

        return bean;
    }
}
