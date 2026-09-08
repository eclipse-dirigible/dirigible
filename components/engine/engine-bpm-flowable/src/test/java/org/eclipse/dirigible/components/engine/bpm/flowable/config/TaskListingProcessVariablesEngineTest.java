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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskSubject;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * That the Inbox's listing really can read a task's subject off the task itself, against a REAL
 * Flowable engine: {@code includeProcessVariables()} populates {@link Task#getProcessVariables()},
 * so the whole listing costs the one task query instead of a variable read per task on every poll
 * (issue #7141). A mirror of the mechanism would not prove it - the engine's own query is the thing
 * being relied on.
 */
class TaskListingProcessVariablesEngineTest {

    private static final String PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="approve" name="Approve" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="approveTask" name="Approve" flowable:assignee="admin"></userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_task" sourceRef="start" targetRef="approveTask"></sequenceFlow>
                        <sequenceFlow id="flow_task_end" sourceRef="approveTask" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static ProcessEngine engine;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:task-listing-variables-test;DB_CLOSE_DELAY=1000");
        engine = configuration.buildProcessEngine();
        engine.getRepositoryService()
              .createDeployment()
              .addString("approve.bpmn20.xml", PROCESS_XML)
              .deploy();
        // Two instances of the same process - the listing has to keep every task's own variables,
        // which is exactly what a single joined query could get wrong.
        engine.getRuntimeService()
              .startProcessInstanceByKey("approve", subjectVariables(6));
        engine.getRuntimeService()
              .startProcessInstanceByKey("approve", subjectVariables(7));
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    private static Map<String, Object> subjectVariables(int id) {
        return Map.of("__entityUrl", "/services/java/billing/gen/billing/api/sales_invoice/SalesInvoiceController", "__entityId", id,
                "__subjectFields", "Number:text,Total:number");
    }

    @Test
    void theSubjectVariablesRideTheTaskQuery() {
        List<Task> tasks = engine.getTaskService()
                                 .createTaskQuery()
                                 .taskAssignee("admin")
                                 .includeProcessVariables()
                                 .list();

        assertEquals(2, tasks.size());
        for (Task task : tasks) {
            TaskSubject subject = TaskSubject.from(task.getProcessVariables());
            assertNotNull(subject, "the subject must be derivable from the variables the query loaded");
            assertEquals(2, subject.fields()
                                   .size());
            // Each task keeps ITS OWN instance's record, not the other one's.
            assertEquals(String.valueOf(task.getProcessVariables()
                                            .get("__entityId")),
                    subject.id());
        }
        assertEquals(2, tasks.stream()
                             .map(task -> task.getProcessVariables()
                                              .get("__entityId"))
                             .distinct()
                             .count(),
                "the two instances' records must not collapse into one");
    }

    /**
     * Why the listing endpoint must ask for the variables: a plain task query leaves
     * {@code getProcessVariables()} empty, which is a subject-less row - never a per-task fallback
     * read, the N+1 this replaced.
     */
    @Test
    void aPlainTaskQueryCarriesNoVariables() {
        List<Task> tasks = engine.getTaskService()
                                 .createTaskQuery()
                                 .taskAssignee("admin")
                                 .list();

        assertTrue(tasks.stream()
                        .allMatch(task -> task.getProcessVariables()
                                              .isEmpty()));
    }
}
