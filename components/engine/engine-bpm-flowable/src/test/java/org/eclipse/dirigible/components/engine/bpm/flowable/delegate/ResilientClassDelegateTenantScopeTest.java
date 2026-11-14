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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.TenantConfigurationScope;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * A {@code flowable:class} step on the async executor runs in the scope of its process instance's
 * tenant, with that tenant's configuration overrides, exactly as a {@code ${JavaTask}} step does
 * (#8549). The executor's threads carry no tenant of their own, so without the scope a class
 * delegate's repositories resolved to the default tenant's schema.
 *
 * <p>
 * A real engine (in-memory, async executor on) runs an async class service task followed by a user
 * task carrying a class execution listener and a class task listener - all three run on the
 * executor thread. The platform's {@link TenantContext} and {@link TenantConfigurationScope} are
 * stood in for by recording doubles behind {@link BeanProvider}, so each client class can record
 * the tenant scope and the overrides it observed.
 */
class ResilientClassDelegateTenantScopeTest {

    private static final String TENANT_ID = "acme";

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> OVERRIDES_APPLIED = ThreadLocal.withInitial(() -> false);

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="tenantScope" name="Tenant Scope" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="check" name="Check" flowable:async="true" flowable:class="%s"></serviceTask>
                        <userTask id="review" name="Review">
                          <extensionElements>
                            <flowable:executionListener event="start" class="%s"></flowable:executionListener>
                            <flowable:taskListener event="create" class="%s"></flowable:taskListener>
                          </extensionElements>
                        </userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_check" sourceRef="start" targetRef="check"></sequenceFlow>
                        <sequenceFlow id="flow_check_review" sourceRef="check" targetRef="review"></sequenceFlow>
                        <sequenceFlow id="flow_review_end" sourceRef="review" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private ProcessEngine engine;

    /** Records the scope its async service task ran in. */
    public static class RecordingDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("delegateScope", observedScope());
        }
    }

    /** Records the scope its execution listener ran in. */
    public static class RecordingExecutionListener implements ExecutionListener {

        @Override
        public void notify(DelegateExecution execution) {
            execution.setVariable("executionListenerScope", observedScope());
        }
    }

    /** Records the scope its task listener ran in. */
    public static class RecordingTaskListener implements TaskListener {

        @Override
        public void notify(DelegateTask task) {
            task.setVariable("taskListenerScope", observedScope());
        }
    }

    private static String observedScope() {
        return CURRENT_TENANT.get() + (OVERRIDES_APPLIED.get() ? "+overrides" : "");
    }

    @BeforeEach
    void startEngineWithinPlatform() {
        new BeanProvider().setApplicationContext(platformContext());

        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:class-delegate-tenant-scope-test;DB_CLOSE_DELAY=1000");
        ResilientClassDelegateFactory classDelegateFactory = new ResilientClassDelegateFactory();
        configuration.setActivityBehaviorFactory(new ResilientActivityBehaviorFactory(classDelegateFactory));
        configuration.setListenerFactory(new DefaultListenerFactory(classDelegateFactory));
        configuration.setAsyncExecutorActivate(true);
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(100));
        engine = configuration.buildProcessEngine();
    }

    @AfterEach
    void stopEngine() {
        if (engine != null) {
            engine.close();
        }
        new BeanProvider().setApplicationContext(null);
    }

    @Test
    void anAsyncClassStepAndItsListenersRunInTheProcessInstancesTenant() {
        engine.getRepositoryService()
              .createDeployment()
              .addString("tenantScope.bpmn20.xml",
                      PROCESS_XML.formatted(RecordingDelegate.class.getName(), RecordingExecutionListener.class.getName(),
                              RecordingTaskListener.class.getName()))
              .tenantId(TENANT_ID)
              .deploy();

        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKeyAndTenantId("tenantScope", TENANT_ID);

        Task review = await().atMost(Duration.ofSeconds(60))
                             .until(() -> engine.getTaskService()
                                                .createTaskQuery()
                                                .processInstanceId(instance.getId())
                                                .singleResult(),
                                     task -> task != null);

        Map<String, Object> variables = engine.getRuntimeService()
                                              .getVariables(instance.getId());
        String expected = TENANT_ID + "+overrides";
        assertEquals(expected, variables.get("delegateScope"), "the async class service task must run in its tenant's scope");
        assertEquals(expected, variables.get("executionListenerScope"), "a class execution listener must run in its tenant's scope");
        assertEquals(expected, variables.get("taskListenerScope"), "a class task listener must run in its tenant's scope");
        assertEquals(TENANT_ID, review.getTenantId());
    }

    /**
     * The platform beans the class delegate reaches through {@link BeanProvider}, as recording doubles.
     */
    @SuppressWarnings("unchecked")
    private static ApplicationContext platformContext() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(invocation -> {
            String previous = CURRENT_TENANT.get();
            CURRENT_TENANT.set(invocation.getArgument(0));
            try {
                return invocation.<CallableResultAndException<Object, Throwable>>getArgument(1)
                                 .call();
            } finally {
                CURRENT_TENANT.set(previous);
            }
        });

        TenantConfigurationScope configurationScope = mock(TenantConfigurationScope.class);
        when(configurationScope.executeWithTenantOverrides(any(CallableResultAndException.class))).thenAnswer(invocation -> {
            boolean previous = OVERRIDES_APPLIED.get();
            OVERRIDES_APPLIED.set(true);
            try {
                return invocation.<CallableResultAndException<Object, Throwable>>getArgument(0)
                                 .call();
            } finally {
                OVERRIDES_APPLIED.set(previous);
            }
        });

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(any(Class.class))).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(0);
            if (type == TenantContext.class) {
                return tenantContext;
            }
            if (type == TenantConfigurationScope.class) {
                return configurationScope;
            }
            // Nothing else is deployed - in particular no client bean container, so the client
            // classes are instantiated plainly.
            throw new NoSuchBeanDefinitionException(type);
        });
        return context;
    }
}
