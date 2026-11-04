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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Suspension suspends only the tenant's active instances - their timers leave the timer table, so
 * nothing of theirs fires - and resuming activates only the tenant's suspended ones, timers back in
 * place (#8477). The engine is a real in-memory Flowable, so the suspension and the jobs are the
 * engine's own, not a mock's.
 */
class BpmTenantSuspensionTest {

    static final String PROCESS_KEY = "suspensionTestReminder";

    /** A process that waits on a timer an hour out - the job a suspension must keep from firing. */
    static final String PROCESS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="http://www.flowable.org/processdef">
              <process id="%s" name="Reminder" isExecutable="true">
                <startEvent id="start"></startEvent>
                <intermediateCatchEvent id="wait">
                  <timerEventDefinition><timeDuration>PT1H</timeDuration></timerEventDefinition>
                </intermediateCatchEvent>
                <endEvent id="end"></endEvent>
                <sequenceFlow id="flow_start_wait" sourceRef="start" targetRef="wait"></sequenceFlow>
                <sequenceFlow id="flow_wait_end" sourceRef="wait" targetRef="end"></sequenceFlow>
              </process>
            </definitions>
            """.formatted(PROCESS_KEY);

    private static ProcessEngine engine;
    private static BpmTenantSuspension suspension;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:tenant-suspension-test;DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();
        suspension = new BpmTenantSuspension(engine);
        for (String tenantId : new String[] {"acme", "globex"}) {
            engine.getRepositoryService()
                  .createDeployment()
                  .addString("reminder.bpmn20.xml", PROCESS_XML)
                  .tenantId(tenantId)
                  .deploy();
        }
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    @AfterEach
    void cancelInstances() {
        engine.getRuntimeService()
              .createProcessInstanceQuery()
              .list()
              .forEach(instance -> {
                  if (instance.isSuspended()) {
                      engine.getRuntimeService()
                            .activateProcessInstanceById(instance.getId());
                  }
                  engine.getRuntimeService()
                        .deleteProcessInstance(instance.getId(), "test cleanup");
              });
    }

    @Test
    void suspendSuspendsOnlyTheTenantsActiveInstancesAndTheirTimers() {
        ProcessInstance acme = start("acme");
        ProcessInstance globex = start("globex");

        suspension.suspend("acme");

        assertTrue(isSuspended(acme), "the tenant's instance is suspended");
        assertFalse(isSuspended(globex), "another tenant's instance runs on");
        assertEquals(0, timers(acme), "the suspended instance's timer cannot fire");
        assertEquals(1, suspendedJobs(acme), "it waits in the suspended-job table");
        assertEquals(1, timers(globex), "another tenant's timer stays scheduled");

        suspension.suspend("acme");
        assertTrue(isSuspended(acme), "a repeated suspend is harmless");
    }

    @Test
    void resumeActivatesOnlyTheTenantsSuspendedInstances() {
        ProcessInstance acme = start("acme");
        ProcessInstance globex = start("globex");
        suspension.suspend("acme");
        engine.getRuntimeService()
              .suspendProcessInstanceById(globex.getId());

        suspension.resume("acme");

        assertFalse(isSuspended(acme), "the tenant's instance is active again");
        assertEquals(1, timers(acme), "with its timer back in place");
        assertTrue(isSuspended(globex), "another tenant's suspended instance is left as it is");

        suspension.resume("acme");
        assertFalse(isSuspended(acme), "a resume of a running tenant is harmless");
    }

    /**
     * A retired deployment suspends its definition and keeps its running instances (#7597). Such an
     * instance of a suspended tenant must still come back, or the tenant could never be resumed.
     */
    @Test
    void anInstanceOfARetiredDefinitionIsActivatedToo() {
        ProcessInstance acme = start("acme");
        suspension.suspend("acme");
        ProcessDefinition definition = engine.getRepositoryService()
                                             .getProcessDefinition(acme.getProcessDefinitionId());
        engine.getRepositoryService()
              .suspendProcessDefinitionById(definition.getId());
        try {
            suspension.resume("acme");

            assertFalse(isSuspended(acme));
        } finally {
            engine.getRepositoryService()
                  .activateProcessDefinitionById(definition.getId());
        }
    }

    static ProcessInstance start(String tenantId) {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(PROCESS_KEY, null, Map.of(), tenantId);
    }

    private static boolean isSuspended(ProcessInstance instance) {
        return engine.getRuntimeService()
                     .createProcessInstanceQuery()
                     .processInstanceId(instance.getId())
                     .singleResult()
                     .isSuspended();
    }

    private static long timers(ProcessInstance instance) {
        return engine.getManagementService()
                     .createTimerJobQuery()
                     .processInstanceId(instance.getId())
                     .count();
    }

    private static long suspendedJobs(ProcessInstance instance) {
        return engine.getManagementService()
                     .createSuspendedJobQuery()
                     .processInstanceId(instance.getId())
                     .count();
    }
}
