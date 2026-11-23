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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.AcquireJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A step cut by a stop is continued once the configured job lock time has passed (#8871), against a
 * REAL Flowable engine (in-memory) configured through {@link BpmFlowableConfig#applyJobLockTimes}.
 * The gone instance is played by the engine's own executor under another lock owner: it acquires
 * the step's job and locks its process instance exactly as a running step does, and never runs it.
 * The executor then starts under a new lock owner - the next instance - and must run the step once
 * both locks have expired, after the configured two seconds instead of Flowable's hour.
 */
class JobLockExpiryEngineTest {

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="cutStep" name="Cut Step" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="call" name="Call" flowable:async="true" flowable:class="%s"></serviceTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_call" sourceRef="start" targetRef="call"></sequenceFlow>
                        <sequenceFlow id="flow_call_end" sourceRef="call" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    /** Records when, and how often, the step ran. */
    public static class StepDelegate implements JavaDelegate {

        static final AtomicInteger RUNS = new AtomicInteger();

        static final AtomicLong RAN_AT = new AtomicLong();

        @Override
        public void execute(DelegateExecution execution) {
            RAN_AT.set(System.currentTimeMillis());
            RUNS.incrementAndGet();
        }
    }

    private ProcessEngine engine;

    @AfterEach
    void stopEngine() {
        if (engine != null) {
            engine.close();
        }
        DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.setStringValue(
                DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.getDefaultValue());
    }

    @Test
    void aStepLockedByAGoneInstanceRunsOnceTheConfiguredLockTimeHasPassed() {
        DirigibleConfig.FLOWABLE_ASYNC_JOB_LOCK_TIME_SECONDS.setStringValue("2");

        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setEngineName("job-lock-expiry-test");
        configuration.setJdbcUrl("jdbc:h2:mem:job-lock-expiry-test;DB_CLOSE_DELAY=1000");
        // Not started at build: the gone instance first takes its locks without running the step.
        configuration.setAsyncExecutorActivate(false);
        BpmFlowableConfig.applyJobLockTimes(configuration);
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(100));
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(100));
        configuration.getAsyncExecutorConfiguration()
                     .setResetExpiredJobsInterval(Duration.ofMillis(250));
        configuration.getAsyncExecutorConfiguration()
                     .setLockOwner("gone-instance");
        engine = configuration.buildProcessEngine();

        engine.getRepositoryService()
              .createDeployment()
              .addString("cutStep.bpmn20.xml", PROCESS_XML.formatted(StepDelegate.class.getName()))
              .deploy();
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("cutStep");

        // The gone instance acquires the step's job and locks its process instance, as the executor
        // does before it runs an exclusive job - then dies without running it.
        long lockedAt = System.currentTimeMillis();
        JobServiceConfiguration jobs = configuration.getJobServiceConfiguration();
        List<? extends JobInfoEntity> acquired = engine.getManagementService()
                                                       .executeCommand(new AcquireJobsCmd(configuration.getAsyncExecutor()));
        assertEquals(1, acquired.size(), "the step's job must be acquired by the gone instance");
        String jobId = acquired.get(0)
                               .getId();
        engine.getManagementService()
              .executeCommand(context -> {
                  jobs.getInternalJobManager()
                      .lockJobScope(jobs.getJobEntityManager()
                                        .findById(jobId));
                  return null;
              });

        JobEntity job = engine.getManagementService()
                              .executeCommand(context -> jobs.getJobEntityManager()
                                                             .findById(jobId));
        // Read through the process instance query: its result map carries the lock columns, the plain
        // execution one does not.
        ExecutionEntity processInstance = (ExecutionEntity) engine.getRuntimeService()
                                                                  .createProcessInstanceQuery()
                                                                  .processInstanceId(instance.getId())
                                                                  .singleResult();
        assertEquals("gone-instance", job.getLockOwner());
        assertEquals("gone-instance", processInstance.getLockOwner(), "the process instance must carry the gone instance's lock");
        Date lockExpiry = job.getLockExpirationTime();
        assertNotNull(lockExpiry);
        assertTrue(lockExpiry.getTime() - lockedAt <= Duration.ofSeconds(10)
                                                              .toMillis(),
                "the lock must expire after the configured 2 seconds, not Flowable's hour: " + lockExpiry);
        assertEquals(lockExpiry, processInstance.getLockTime(), "the process instance's lock must expire with the job's");

        // The next instance: another lock owner, the executor running.
        configuration.getAsyncExecutorConfiguration()
                     .setLockOwner("next-instance");
        configuration.getAsyncExecutor()
                     .start();

        await().atMost(Duration.ofSeconds(30))
               .pollInterval(Duration.ofMillis(100))
               .until(() -> engine.getHistoryService()
                                  .createHistoricProcessInstanceQuery()
                                  .processInstanceId(instance.getId())
                                  .finished()
                                  .count() == 1);
        assertEquals(1, StepDelegate.RUNS.get(), "the cut step must run exactly once");
        assertTrue(StepDelegate.RAN_AT.get() >= lockExpiry.getTime(),
                "the step must not run while the gone instance's lock still holds: ran at " + new Date(StepDelegate.RAN_AT.get())
                        + ", lock expired at " + lockExpiry);
    }
}
