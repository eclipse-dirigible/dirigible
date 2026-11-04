/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.EarlyMessageJobHandler;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.EarlyMessages;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A message that reaches an instance before it is parked on the catch event waiting for it (#8106),
 * on a REAL Flowable engine (in-memory, async executor on). The process mirrors an intent wait
 * after a user task: {@code work} (user task) -> {@code transit} (an asynchronous step, held open
 * by the test) -> {@code pause} (a timer) -> {@code awaitReply} (the message catch event, with the
 * listeners the intent generator emits) -> {@code replied}. Holding {@code transit} open, or
 * delivering during {@code pause}, puts the delivery deterministically into the window the issue
 * lost it in: after the user task, before the wait subscribed.
 */
class EarlyMessageEngineTest {

    private static final String TENANT_ID = "early-message-tenant";
    private static final String PROCESS_KEY = "earlyMessage";
    private static final String MESSAGE = "EarlyMessageAwaitReply";

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <message id="%2$s" name="%2$s"></message>
                      <process id="%1$s" name="Early message" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="work" name="Work">
                          <extensionElements>
                            <flowable:executionListener event="end" expression="${EarlyMessages.discardAll(execution)}"></flowable:executionListener>
                          </extensionElements>
                        </userTask>
                        <serviceTask id="transit" name="Transit" flowable:async="true" flowable:class="%3$s"></serviceTask>
                        <intermediateCatchEvent id="pause" name="Pause">
                          <timerEventDefinition><timeDuration>PT1S</timeDuration></timerEventDefinition>
                        </intermediateCatchEvent>
                        <intermediateCatchEvent id="awaitReply" name="Await Reply">
                          <extensionElements>
                            <flowable:executionListener event="start" expression="${EarlyMessages.checkWhenArmed(execution)}"></flowable:executionListener>
                            <flowable:executionListener event="end" expression="${EarlyMessages.consumed(execution)}"></flowable:executionListener>
                          </extensionElements>
                          <messageEventDefinition messageRef="%2$s"></messageEventDefinition>
                        </intermediateCatchEvent>
                        <serviceTask id="replied" name="Replied" flowable:class="%4$s"></serviceTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="f1" sourceRef="start" targetRef="work"></sequenceFlow>
                        <sequenceFlow id="f2" sourceRef="work" targetRef="transit"></sequenceFlow>
                        <sequenceFlow id="f3" sourceRef="transit" targetRef="pause"></sequenceFlow>
                        <sequenceFlow id="f3a" sourceRef="pause" targetRef="awaitReply"></sequenceFlow>
                        <sequenceFlow id="f4" sourceRef="awaitReply" targetRef="replied"></sequenceFlow>
                        <sequenceFlow id="f5" sourceRef="replied" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """.formatted(
                    PROCESS_KEY, MESSAGE, TransitTask.class.getName(), RepliedTask.class.getName());

    private static ProcessEngine engine;
    private static BpmProviderFlowable provider;

    /** Holds the instance between the user task and the wait until the test lets it go. */
    public static class TransitTask implements JavaDelegate {

        static final Semaphore ENTERED = new Semaphore(0);
        static final Semaphore RELEASE = new Semaphore(0);

        @Override
        public void execute(DelegateExecution execution) {
            ENTERED.release();
            try {
                if (!RELEASE.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the test never released the transit step");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException(interrupted);
            }
        }
    }

    /** Records that the wait was resumed. */
    public static class RepliedTask implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("replied", Boolean.TRUE);
        }
    }

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        // The lock timeout of the platform's own H2 databases: a write to an instance an asynchronous
        // step is moving waits for that step to commit, as it does on PostgreSQL.
        configuration.setJdbcUrl("jdbc:h2:mem:early-message-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        configuration.setAsyncExecutorActivate(true);
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(100));
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(100));
        configuration.setBeans(Map.<Object, Object>of(EarlyMessages.BEAN_NAME, new EarlyMessages()));
        configuration.setCustomJobHandlers(List.of(new EarlyMessageJobHandler()));
        engine = configuration.buildProcessEngine();
        engine.getRepositoryService()
              .createDeployment()
              .tenantId(TENANT_ID)
              .addString("earlyMessage.bpmn20.xml", PROCESS_XML)
              .deploy();
        TenantContext tenantContext = tenantContext();
        provider = new BpmProviderFlowable(mock(IRepository.class), tenantContext, new FlowableArtefactsValidator(engine, tenantContext),
                engine);
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    @BeforeEach
    void resetTransit() {
        TransitTask.ENTERED.drainPermits();
        TransitTask.RELEASE.drainPermits();
    }

    @Test
    void aMessageArrivingWhileAStepBeforeTheWaitRunsResumesTheWaitOnceArmed() throws InterruptedException {
        ProcessInstance instance = start();
        complete(instance);
        assertTrue(TransitTask.ENTERED.tryAcquire(30, TimeUnit.SECONDS), "the instance should be running its transit step");

        // The step holds the instance until it commits, so keeping the message waits for it - the
        // step is let go from elsewhere, and the delivery then finds the instance parked on the timer.
        CompletableFuture<Void> release = CompletableFuture.runAsync(() -> TransitTask.RELEASE.release(),
                CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS));
        provider.deliverMessageEvent(instance.getId(), MESSAGE);
        release.join();

        awaitEnded(instance);
        assertEquals(Boolean.TRUE, repliedOf(instance), "the kept message must resume the wait once it is armed");
    }

    @Test
    void aMessageArrivingWhileTheInstanceIsParkedElsewhereResumesTheWaitOnceArmed() {
        ProcessInstance instance = start();
        TransitTask.RELEASE.release();
        complete(instance);
        awaitActivities(instance, List.of("pause"));

        provider.deliverMessageEvent(instance.getId(), MESSAGE);

        awaitEnded(instance);
        assertEquals(Boolean.TRUE, repliedOf(instance), "the kept message must resume the wait once it is armed");
    }

    @Test
    void aMessageArrivingWhileThePersonStillWorksDoesNotResumeTheLaterWait() {
        ProcessInstance instance = start();
        provider.deliverMessageEvent(instance.getId(), MESSAGE);
        TransitTask.RELEASE.release();
        complete(instance);

        // Parked on the wait, and the check its arming scheduled has run.
        awaitActivities(instance, List.of("awaitReply"));
        await().atMost(Duration.ofSeconds(30))
               .pollInterval(Duration.ofMillis(50))
               .until(() -> engine.getManagementService()
                                  .createJobQuery()
                                  .processInstanceId(instance.getId())
                                  .count() == 0);
        assertEquals(List.of("awaitReply"), engine.getRuntimeService()
                                                  .getActiveActivityIds(instance.getId()),
                "a message from before the task was completed must not answer the wait after it");
        assertNull(engine.getRuntimeService()
                         .getVariable(instance.getId(), "replied"));

        provider.deliverMessageEvent(instance.getId(), MESSAGE);
        awaitEnded(instance);
        assertEquals(Boolean.TRUE, repliedOf(instance), "a message to the parked wait resumes it at once");
    }

    @Test
    void aMessageForAnInstanceThatIsNotRunningIsRefusedByType() {
        assertThrows(IllegalArgumentException.class, () -> provider.deliverMessageEvent("no-such-instance", MESSAGE));
    }

    private static ProcessInstance start() {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(PROCESS_KEY, null, Map.of(), TENANT_ID);
    }

    private static void complete(ProcessInstance instance) {
        Task task = engine.getTaskService()
                          .createTaskQuery()
                          .processInstanceId(instance.getId())
                          .singleResult();
        engine.getTaskService()
              .complete(task.getId());
    }

    private static void awaitActivities(ProcessInstance instance, List<String> activities) {
        await().atMost(Duration.ofSeconds(30))
               .pollInterval(Duration.ofMillis(50))
               .until(() -> engine.getRuntimeService()
                                  .getActiveActivityIds(instance.getId())
                                  .equals(activities));
    }

    private static void awaitEnded(ProcessInstance instance) {
        await().atMost(Duration.ofSeconds(30))
               .pollInterval(Duration.ofMillis(50))
               .until(() -> engine.getHistoryService()
                                  .createHistoricProcessInstanceQuery()
                                  .processInstanceId(instance.getId())
                                  .finished()
                                  .count() == 1);
    }

    private static Object repliedOf(ProcessInstance instance) {
        return engine.getHistoryService()
                     .createHistoricVariableInstanceQuery()
                     .processInstanceId(instance.getId())
                     .variableName("replied")
                     .singleResult()
                     .getValue();
    }

    /** The provider resolves its tenant per call; it must match the one the instances run in. */
    private static TenantContext tenantContext() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(TENANT_ID);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        return tenantContext;
    }
}
