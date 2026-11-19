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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
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
 * Two concurrent completions of one task: only the one that succeeds runs the service step after
 * the task (#8547), on a REAL Flowable engine (in-memory H2 with the platform's lock timeout).
 *
 * <p>
 * The step stands for a generated {@code JavaTask} delegate - a number stamp, a status setter - whose
 * write goes to the application's records outside the engine's transaction: it is recorded in a
 * plain list, which the engine's rollback cannot undo, exactly like a committed data-store write.
 * The first completion is held inside the step; the second is started only then, so without the
 * lock it has nothing to wait for and runs the step too (the list ends with both actions), while
 * with the lock it waits on the task row until the first one commits and then fails without
 * running it.
 */
class TaskCompletionLockEngineTest {

    private static final String PROCESS_KEY = "completionLock";

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%1$s" name="Completion lock" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="decide" name="Decide"></userTask>
                        <serviceTask id="stamp" name="Stamp" flowable:class="%2$s"></serviceTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="f1" sourceRef="start" targetRef="decide"></sequenceFlow>
                        <sequenceFlow id="f2" sourceRef="decide" targetRef="stamp"></sequenceFlow>
                        <sequenceFlow id="f3" sourceRef="stamp" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """.formatted(
                    PROCESS_KEY, StampTask.class.getName());

    private static ProcessEngine engine;

    /** Records the action it ran for outside the engine, and holds when the test asks it to. */
    public static class StampTask implements JavaDelegate {

        static final List<Object> WRITES = new CopyOnWriteArrayList<>();
        static final Semaphore ENTERED = new Semaphore(0);
        static final Semaphore RELEASE = new Semaphore(0);
        static volatile boolean hold;

        @Override
        public void execute(DelegateExecution execution) {
            WRITES.add(execution.getVariable("action"));
            ENTERED.release();
            if (!hold) {
                return;
            }
            try {
                if (!RELEASE.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the test never released the step");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException(interrupted);
            }
        }
    }

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        // The lock timeout of the platform's own H2 databases.
        configuration.setJdbcUrl("jdbc:h2:mem:completion-lock-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        // The production wiring (BpmFlowableConfig).
        TaskCompletionLock.register(configuration);
        engine = configuration.buildProcessEngine();
        engine.getRepositoryService()
              .createDeployment()
              .addString("completionLock.bpmn20.xml", PROCESS_XML)
              .deploy();
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    @BeforeEach
    void reset() {
        StampTask.WRITES.clear();
        StampTask.ENTERED.drainPermits();
        StampTask.RELEASE.drainPermits();
        StampTask.hold = false;
    }

    @Test
    void aCompletionLosingTheRaceRunsNoStepOfItsOwn() throws Exception {
        ProcessInstance instance = start();
        Task task = taskOf(instance);
        StampTask.hold = true;

        CompletableFuture<Void> approve = CompletableFuture.runAsync(() -> complete(task, "approve"));
        assertTrue(StampTask.ENTERED.tryAcquire(30, TimeUnit.SECONDS), "the first completion should be running its step");

        // The first completion is inside its step and has written nothing to the engine yet; the
        // second one starts now. Without the lock nothing stops it from reaching the step.
        CompletableFuture<Void> reject = CompletableFuture.runAsync(() -> complete(task, "reject"));
        boolean secondRanTheStep = StampTask.ENTERED.tryAcquire(3, TimeUnit.SECONDS);
        StampTask.RELEASE.release(2);

        Throwable approveFailure = failureOf(approve);
        Throwable rejectFailure = failureOf(reject);

        // Without the lock both ran the step, and whichever flushed second failed: the list then holds
        // both actions while only one completion answered success.
        assertEquals(List.of("approve"), StampTask.WRITES, "only the completion that succeeded may have written - approve "
                + (approveFailure == null ? "succeeded" : "failed") + ", reject " + (rejectFailure == null ? "succeeded" : "failed"));
        assertFalse(secondRanTheStep, "the losing completion must not reach the step while the first one runs it");
        assertNull(approveFailure, "the first completion succeeds");
        assertNotNull(rejectFailure, "the losing completion must fail");
        assertTrue(rejectFailure instanceof FlowableOptimisticLockingException, "the engine's optimistic-lock failure, which the endpoints answer with 409: " + rejectFailure);
        assertNull(engine.getRuntimeService()
                         .createProcessInstanceQuery()
                         .processInstanceId(instance.getId())
                         .singleResult(),
                "the instance went on once, to its end");
    }

    @Test
    void aSingleCompletionRunsItsStepOnce() {
        ProcessInstance instance = start();
        Task task = taskOf(instance);
        // A claim moves the task to its next revision first - the lock must expect that one.
        engine.getTaskService()
              .claim(task.getId(), "approver");

        complete(task, "approve");

        assertEquals(List.of("approve"), StampTask.WRITES);
        assertNull(engine.getRuntimeService()
                         .createProcessInstanceQuery()
                         .processInstanceId(instance.getId())
                         .singleResult());
    }

    @Test
    void aCompletionAfterTheTaskWasCompletedIsRefusedWithoutAStep() {
        ProcessInstance instance = start();
        Task task = taskOf(instance);
        complete(task, "approve");

        FlowableException refused = assertThrows(FlowableException.class, () -> complete(task, "reject"));

        assertNotNull(refused);
        assertEquals(List.of("approve"), StampTask.WRITES);
    }

    private static Throwable failureOf(CompletableFuture<Void> completion) throws Exception {
        try {
            completion.get(30, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    private static ProcessInstance start() {
        return engine.getRuntimeService()
                     .startProcessInstanceByKey(PROCESS_KEY);
    }

    private static Task taskOf(ProcessInstance instance) {
        return engine.getTaskService()
                     .createTaskQuery()
                     .processInstanceId(instance.getId())
                     .singleResult();
    }

    private static void complete(Task task, String action) {
        engine.getTaskService()
              .complete(task.getId(), Map.of("action", action));
    }
}
