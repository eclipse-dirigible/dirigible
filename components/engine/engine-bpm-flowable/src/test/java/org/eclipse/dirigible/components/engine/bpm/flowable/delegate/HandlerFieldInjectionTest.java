/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;
import org.flowable.common.engine.impl.el.FixedValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * #8392: Flowable resolves {@code ${JavaTask}} (and {@code ${JSTask}}, {@code ${JSListener}}) by
 * bean name on every execution and then INJECTS the step's {@code handler} field into what it got
 * back, before calling it. Two async jobs doing that at once on one shared instance let the second
 * injection land before the first job reads its handler - a "notify" instance's send step ran the
 * mark-done handler of another instance's job, and the record ended Provisioned with no mail sent.
 * The interleaving is forced here in the order Flowable performs the steps.
 */
class HandlerFieldInjectionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withBean(TenantContext.class, () -> mock(TenantContext.class))
                                          .withBean(ClientClassLoaderHolder.class, () -> mock(ClientClassLoaderHolder.class))
                                          .withUserConfiguration(DirigibleJavaCallDelegate.class, DirigibleCallDelegate.class,
                                                  DirigibleTaskListener.class);

    @ParameterizedTest
    @ValueSource(strings = {"JavaTask", "JSTask", "JSListener"})
    void aConcurrentStepsHandlerNeverReachesThisStep(String beanName) {
        contextRunner.run(context -> {
            Object thisStep = context.getBean(beanName);
            new BeanWrapperImpl(thisStep).setPropertyValue("handler", new FixedValue("gen.events.NotifyOwnerSend"));

            Object concurrentStep = context.getBean(beanName);
            new BeanWrapperImpl(concurrentStep).setPropertyValue("handler", new FixedValue("gen.events.MarkDone"));

            FixedValue handler = (FixedValue) new BeanWrapperImpl(thisStep).getPropertyValue("handler");
            assertThat(handler.getExpressionText()).isEqualTo("gen.events.NotifyOwnerSend");
        });
    }
}
