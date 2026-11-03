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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * What a task completion may write into its process, against a REAL Flowable engine (in-memory)
 * (issue #8094). The delegates of an intent-generated process find their record by the key variable
 * and the Inbox presents a task through the {@code __} locators, so a completion carrying another
 * record's {@code Id} moved the process's writes onto that record, and one carrying a field its
 * form does not offer planted it for a later step's write-back.
 */
class BpmServiceCompleteTaskTest {

    private static final String TENANT_ID = "complete-task-tenant";

    private static final String DECLARING_PROCESS = "declaring";
    private static final String HAND_AUTHORED_PROCESS = "hand-authored";
    private static final String SEPARATED_PROCESS = "separated";

    /** The user {@code UserFacade} names outside a request - who every completion here is made by. */
    private static final String USER = "guest";

    /** An approval its submitter may not give (issue #8111). */
    private static final String SEPARATED_PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="separated" name="Separated" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="submit" name="Submit"></userTask>
                        <userTask id="approve" name="Approve">
                          <extensionElements>
                            <flowable:property name="taskSeparateFrom" value="submit"></flowable:property>
                            <flowable:property name="taskSeparationMessage" value="You cannot approve your own claim"></flowable:property>
                          </extensionElements>
                        </userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_submit" sourceRef="start" targetRef="submit"></sequenceFlow>
                        <sequenceFlow id="flow_submit_approve" sourceRef="submit" targetRef="approve"></sequenceFlow>
                        <sequenceFlow id="flow_approve_end" sourceRef="approve" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    /** Two tasks: the first may write nothing but its action, the second its editable Solution. */
    private static final String DECLARING_PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="declaring" name="Declaring" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="triage" name="Triage">
                          <extensionElements>
                            <flowable:property name="taskActions" value="confirm"></flowable:property>
                            <flowable:property name="taskWrites" value=""></flowable:property>
                          </extensionElements>
                        </userTask>
                        <userTask id="resolve" name="Resolve">
                          <extensionElements>
                            <flowable:property name="taskWrites" value="Solution"></flowable:property>
                          </extensionElements>
                        </userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_triage" sourceRef="start" targetRef="triage"></sequenceFlow>
                        <sequenceFlow id="flow_triage_resolve" sourceRef="triage" targetRef="resolve"></sequenceFlow>
                        <sequenceFlow id="flow_resolve_end" sourceRef="resolve" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static final String HAND_AUTHORED_PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="hand-authored" name="Hand Authored" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="review" name="Review"></userTask>
                        <userTask id="wait" name="Wait"></userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_review" sourceRef="start" targetRef="review"></sequenceFlow>
                        <sequenceFlow id="flow_review_wait" sourceRef="review" targetRef="wait"></sequenceFlow>
                        <sequenceFlow id="flow_wait_end" sourceRef="wait" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static ProcessEngine engine;
    private static BpmService bpmService;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        // A database of this test's own: another engine test in the same surefire JVM keeps its
        // in-memory one alive past the class that built it.
        configuration.setJdbcUrl("jdbc:h2:mem:complete-task-test;DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();

        deploy(DECLARING_PROCESS, DECLARING_PROCESS_XML);
        deploy(HAND_AUTHORED_PROCESS, HAND_AUTHORED_PROCESS_XML);
        deploy(SEPARATED_PROCESS, SEPARATED_PROCESS_XML);

        BpmProviderFlowable bpmProvider =
                new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class), engine);
        bpmService = new BpmService(mock(ObjectProvider.class), bpmProvider, mock(ObjectProvider.class));
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void aCompletionWritesNeitherTheKeyNorTheLocatorsNorAFieldItsFormDoesNotOffer() {
        ProcessInstance instance = start(DECLARING_PROCESS, Map.of("Id", 7, "__entityId", 7));

        bpmService.completeTask(taskOf(instance), completion(Map.of("action", "confirm", "Id", 5, "__entityId", 5, "Solution", "planted")));

        Map<String, Object> variables = variablesOf(instance);
        assertEquals(7, variables.get("Id"), "the key the delegates find the record by must stay the instance's own");
        assertEquals(7, variables.get("__entityId"), "the locator the Inbox presents the record by must stay the instance's own");
        assertEquals("confirm", variables.get("action"), "the action is what a completion is for");
        assertFalse(variables.containsKey("Solution"), "a field the triage form does not offer must not be planted for resolve");
    }

    @Test
    void aCompletionWritesTheFieldsItsTaskDeclares() {
        ProcessInstance instance = start(DECLARING_PROCESS, Map.of("Id", 7));
        bpmService.completeTask(taskOf(instance), completion(Map.of("action", "confirm")));

        bpmService.completeTask(taskOf(instance), completion(Map.of("action", "resolve", "Solution", "new drum", "Id", 5)));

        assertEquals("new drum", historicVariable(instance, "Solution"), "the resolve form's editable field is its to write");
        assertEquals(7, historicVariable(instance, "Id"), "the key stays the instance's own");
    }

    @Test
    void aHandAuthoredTaskWritesWhatItIsSentButThePlatformsOwnVariables() {
        ProcessInstance instance = start(HAND_AUTHORED_PROCESS, Map.of("__entityId", 7));

        bpmService.completeTask(taskOf(instance), completion(Map.of("decision", "approve", "Id", 5, "__entityId", 5)));

        Map<String, Object> variables = variablesOf(instance);
        assertEquals("approve", variables.get("decision"), "a task declaring no writes decides what it takes");
        assertEquals(5, variables.get("Id"), "a task declaring no writes decides what it takes");
        assertEquals(7, variables.get("__entityId"), "a __ variable is the platform's own, whatever the task declares");
    }

    @Test
    void aCompletionWithoutDataCompletesTheTask() {
        ProcessInstance instance = start(DECLARING_PROCESS, Map.of("Id", 7));
        String triage = taskOf(instance);

        bpmService.completeTask(triage, null);

        assertEquals("resolve", engine.getTaskService()
                                      .createTaskQuery()
                                      .processInstanceId(instance.getId())
                                      .singleResult()
                                      .getTaskDefinitionKey());
    }

    @Test
    void whoeverCompletedTheSubmissionMayNeitherClaimNorCompleteTheApproval() {
        ProcessInstance instance = start(SEPARATED_PROCESS, Map.of());
        bpmService.completeTask(taskOf(instance), completion(Map.of("__completedBy_submit", "someone else")));
        assertEquals(USER, variablesOf(instance).get("__completedBy_submit"), "the completion records its own user, never a sent one");
        String approve = taskOf(instance);

        ResponseStatusException completion = assertThrows(ResponseStatusException.class, () -> bpmService.completeTask(approve, null));
        ResponseStatusException claim = assertThrows(ResponseStatusException.class, () -> bpmService.claimTask(approve, USER));

        assertEquals(HttpStatus.FORBIDDEN, completion.getStatusCode());
        assertEquals("You cannot approve your own claim", completion.getReason());
        assertEquals(HttpStatus.FORBIDDEN, claim.getStatusCode());
        assertEquals(approve, taskOf(instance), "the approval stays open for someone else");
    }

    @Test
    void anotherUsersSubmissionLeavesTheApprovalOpen() {
        ProcessInstance instance = start(SEPARATED_PROCESS, Map.of());
        engine.getTaskService()
              .complete(taskOf(instance), Map.of("__completedBy_submit", "requester"));

        bpmService.completeTask(taskOf(instance), null);

        assertEquals(0, engine.getRuntimeService()
                              .createProcessInstanceQuery()
                              .processInstanceId(instance.getId())
                              .count());
    }

    /** A mutable map, as Jackson hands the endpoint a completion's data. */
    private static Map<String, Object> completion(Map<String, Object> data) {
        return new HashMap<>(data);
    }

    private static String taskOf(ProcessInstance instance) {
        return engine.getTaskService()
                     .createTaskQuery()
                     .processInstanceId(instance.getId())
                     .singleResult()
                     .getId();
    }

    private static Map<String, Object> variablesOf(ProcessInstance instance) {
        return engine.getRuntimeService()
                     .getVariables(instance.getId());
    }

    private static Object historicVariable(ProcessInstance instance, String name) {
        return engine.getHistoryService()
                     .createHistoricVariableInstanceQuery()
                     .processInstanceId(instance.getId())
                     .variableName(name)
                     .singleResult()
                     .getValue();
    }

    private static ProcessInstance start(String processKey, Map<String, Object> variables) {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(processKey, null, variables, TENANT_ID);
    }

    private static void deploy(String processKey, String xml) {
        engine.getRepositoryService()
              .createDeployment()
              .addString(processKey + ".bpmn20.xml", xml)
              .tenantId(TENANT_ID)
              .deploy();
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
