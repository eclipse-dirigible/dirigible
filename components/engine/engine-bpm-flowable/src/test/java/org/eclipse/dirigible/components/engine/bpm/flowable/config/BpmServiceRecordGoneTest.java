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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.ProcessRecordGuard;
import org.eclipse.dirigible.components.workspace.service.UserWorkspaces;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * A task of a record that has been deleted is not completed: its process is retired instead,
 * against a REAL Flowable engine (in-memory) (issue #8108).
 */
class BpmServiceRecordGoneTest {

    private static final String TENANT_ID = "record-gone-tenant";

    private static final String PROCESS = "TicketHandling";

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="TicketHandling" name="Ticket Handling" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="triage" name="Triage"></userTask>
                        <userTask id="work" name="Work"></userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_triage" sourceRef="start" targetRef="triage"></sequenceFlow>
                        <sequenceFlow id="flow_triage_work" sourceRef="triage" targetRef="work"></sequenceFlow>
                        <sequenceFlow id="flow_work_end" sourceRef="work" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    /** The records the guard reports deleted, as process key + record id. */
    private static final Set<String> DELETED = new HashSet<>();

    /** Every question the guard was asked. */
    private static final List<String> ASKED = new ArrayList<>();

    private static ProcessEngine engine;
    private static BpmService bpmService;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:record-gone-test;DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();
        engine.getRepositoryService()
              .createDeployment()
              .addString(PROCESS + ".bpmn20.xml", PROCESS_XML)
              .tenantId(TENANT_ID)
              .deploy();

        ProcessRecordGuard guard = (processDefinitionKey, recordId) -> {
            ASKED.add(processDefinitionKey + ":" + recordId);
            return DELETED.contains(processDefinitionKey + ":" + recordId);
        };
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessRecordGuard> guards = mock(ObjectProvider.class);
        when(guards.orderedStream()).thenAnswer(invocation -> Stream.of(guard));

        BpmProviderFlowable bpmProvider =
                new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class), engine);
        @SuppressWarnings("unchecked")
        ObjectProvider<UserWorkspaces> workspaces = mock(ObjectProvider.class);
        bpmService = new BpmService(workspaces, bpmProvider, guards);
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @BeforeEach
    void reset() {
        DELETED.clear();
        ASKED.clear();
    }

    @Test
    void aTaskWhoseRecordWasDeletedRetiresItsProcess() {
        ProcessInstance instance = start(Map.of("Id", 149, "__entityId", 149));
        DELETED.add(PROCESS + ":149");

        assertTrue(bpmService.retireIfRecordGone(taskOf(instance)), "the task of a deleted record must be refused");

        assertNull(engine.getRuntimeService()
                         .createProcessInstanceQuery()
                         .processInstanceId(instance.getId())
                         .singleResult(),
                "the process of the deleted record must be cancelled - no next task is handed out for it");
        assertEquals(List.of(PROCESS + ":149"), ASKED);
    }

    @Test
    void aTaskWhoseRecordExistsIsLeftToBeCompleted() {
        ProcessInstance instance = start(Map.of("Id", 150, "__entityId", 150));

        assertFalse(bpmService.retireIfRecordGone(taskOf(instance)));

        assertNotNull(engine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceId(instance.getId())
                            .singleResult(),
                "the process of an existing record keeps running");
    }

    @Test
    void aProcessCarryingNoRecordIsNeverAsked() {
        ProcessInstance instance = start(Map.of("Id", 151));

        assertFalse(bpmService.retireIfRecordGone(taskOf(instance)));
        assertEquals(List.of(), ASKED, "a hand-authored process carries no record locator to look up");
    }

    @Test
    void anUnknownTaskIsNeverAsked() {
        assertFalse(bpmService.retireIfRecordGone("no-such-task"));
        assertEquals(List.of(), ASKED);
    }

    private static String taskOf(ProcessInstance instance) {
        return engine.getTaskService()
                     .createTaskQuery()
                     .processInstanceId(instance.getId())
                     .singleResult()
                     .getId();
    }

    private static ProcessInstance start(Map<String, Object> variables) {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(PROCESS, null, variables, TENANT_ID);
    }

    private static TenantContext tenantContext() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(TENANT_ID);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        return tenantContext;
    }
}
