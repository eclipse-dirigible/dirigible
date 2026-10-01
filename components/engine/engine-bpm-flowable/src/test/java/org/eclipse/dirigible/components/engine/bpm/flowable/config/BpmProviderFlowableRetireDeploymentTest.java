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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A deployment is retired, never cascaded (#7597): with running instances it is kept suspended, so
 * they complete and nothing new starts from it; without, it is deleted and its history stays for
 * the audit (#7598). The engine is a real in-memory Flowable, so the suspension and the history are
 * the engine's own, not a mock's.
 */
class BpmProviderFlowableRetireDeploymentTest {

    private static final String TENANT_ID = "retire-test-tenant";
    private static final String DEPLOYMENT_KEY = "/retire-test/approval.bpmn";
    private static final String PROCESS_KEY = "retireTestApproval";

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="Approval" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="approve" name="Approve"></userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_task" sourceRef="start" targetRef="approve"></sequenceFlow>
                        <sequenceFlow id="flow_task_end" sourceRef="approve" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """.formatted(
                    PROCESS_KEY);

    private static ProcessEngine engine;
    private static ProcessEngine engineWithoutHistory;
    private static BpmProviderFlowable provider;
    private static BpmProviderFlowable providerWithoutHistory;

    @BeforeAll
    static void startEngines() {
        engine = buildEngine("jdbc:h2:mem:retire-deployment-test;DB_CLOSE_DELAY=-1", HistoryLevel.AUDIT);
        engineWithoutHistory = buildEngine("jdbc:h2:mem:retire-deployment-no-history-test;DB_CLOSE_DELAY=-1", HistoryLevel.NONE);
        provider = new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class), engine);
        providerWithoutHistory = new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class),
                engineWithoutHistory);
    }

    @AfterAll
    static void stopEngines() {
        engine.close();
        engineWithoutHistory.close();
    }

    @Test
    void aDeploymentWithRunningInstancesIsKeptSuspended() {
        Deployment deployment = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance running = start();

        assertFalse(provider.retireDeployment(deployment.getId()), "a deployment with work in flight is kept");

        assertNotNull(provider.getProcessInstance(running.getId()), "the running instance survives the retire");
        assertTrue(provider.isDeploymentRetired(deployment.getId()), "its definition is suspended");
        assertThrows(FlowableException.class, BpmProviderFlowableRetireDeploymentTest::start,
                "no new instance starts from a retired definition");
        assertEquals(1, engine.getRepositoryService()
                              .createDeploymentQuery()
                              .deploymentId(deployment.getId())
                              .count(),
                "the deployment itself is still there");

        complete(running);
        assertTrue(provider.retireDeployment(deployment.getId()), "once its last instance ended, the retire deletes it");
        assertEquals(0, deployments(), "and nothing of the key remains deployed");
        assertTrue(provider.isProcessInstanceKnown(running.getId()), "the ended instance is still in the history");
    }

    @Test
    void anIdleDeploymentIsDeletedWithItsHistoryKept() {
        Deployment deployment = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance ended = start();
        complete(ended);

        assertTrue(provider.retireDeployment(deployment.getId()));

        assertEquals(0, deployments());
        assertNotNull(engine.getHistoryService()
                            .createHistoricProcessInstanceQuery()
                            .processInstanceId(ended.getId())
                            .singleResult(),
                "a non-cascading delete keeps the finished instance in the history");
    }

    @Test
    void aRedeployOfTheKeyIsTheNextVersionAndNewInstancesStartFromIt() {
        Deployment retired = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance running = start();
        provider.retireDeployment(retired.getId());

        Deployment redeployed = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessDefinition latest = provider.getProcessDefinitionByDeploymentId(redeployed.getId());
        assertFalse(latest.isSuspended(), "the redeployed definition is live");
        ProcessInstance fresh = start();
        assertEquals(latest.getId(), fresh.getProcessDefinitionId(), "a new instance starts from the latest version");
        assertNotNull(provider.getProcessInstance(running.getId()), "the instance of the retired version keeps running");

        complete(running);
        complete(fresh);
        provider.retireDeployment(retired.getId());
        provider.retireDeployment(redeployed.getId());
        assertEquals(0, deployments());
    }

    @Test
    void anExplicitUndeployRefusesWhileInstancesRun() {
        Deployment deployment = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance running = start();

        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> provider.undeployProcess(deployment.getId()));
        assertTrue(refused.getMessage()
                          .contains(running.getId()),
                "the refusal names the running instance: " + refused.getMessage());
        assertNotNull(provider.getProcessInstance(running.getId()));

        complete(running);
        provider.undeployProcess(deployment.getId());
        assertEquals(0, deployments());
    }

    @Test
    void anInstanceIsKnownWhileRunningAndFromTheHistoryAfterwards() {
        Deployment deployment = provider.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance instance = start();
        assertTrue(provider.isProcessInstanceKnown(instance.getId()));

        complete(instance);
        assertTrue(provider.isProcessInstanceKnown(instance.getId()), "finished, but recorded");
        assertFalse(provider.isProcessInstanceKnown("no-such-instance"), "what the engine never had");

        engine.getHistoryService()
              .deleteHistoricProcessInstance(instance.getId());
        assertFalse(provider.isProcessInstanceKnown(instance.getId()), "gone from the history as well: a dangling stamp");
        provider.retireDeployment(deployment.getId());
    }

    @Test
    void withoutHistoryAFinishedInstanceCannotBeToldFromALostOneSoTheStampIsTrusted() {
        Deployment deployment = providerWithoutHistory.deployProcess(DEPLOYMENT_KEY, "approval.bpmn20.xml", PROCESS_XML);
        ProcessInstance instance = engineWithoutHistory.getRuntimeService()
                                                       .startProcessInstanceByKeyAndTenantId(PROCESS_KEY, null, Map.of(), TENANT_ID);
        Task task = engineWithoutHistory.getTaskService()
                                        .createTaskQuery()
                                        .processInstanceId(instance.getId())
                                        .singleResult();
        engineWithoutHistory.getTaskService()
                            .complete(task.getId());

        assertNull(providerWithoutHistory.getProcessInstance(instance.getId()), "not running any more");
        assertEquals(HistoryLevel.NONE, providerWithoutHistory.getHistoryLevel());
        assertTrue(providerWithoutHistory.isProcessInstanceKnown(instance.getId()), "no history to consult: assumed finished");
        providerWithoutHistory.retireDeployment(deployment.getId());
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

    private static long deployments() {
        return engine.getRepositoryService()
                     .createDeploymentQuery()
                     .deploymentKey(DEPLOYMENT_KEY)
                     .count();
    }

    private static ProcessEngine buildEngine(String jdbcUrl, HistoryLevel historyLevel) {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        // A database of this test's own: another engine test in the same surefire JVM keeps its
        // in-memory one alive past the class that built it.
        configuration.setJdbcUrl(jdbcUrl);
        configuration.setHistory(historyLevel.getKey());
        return configuration.buildProcessEngine();
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
