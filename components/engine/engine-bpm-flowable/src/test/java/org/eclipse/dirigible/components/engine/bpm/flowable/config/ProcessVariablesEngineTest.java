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

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskSubject;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * That a key started as a JSON number survives Flowable's variable store as the whole number it is,
 * against a REAL engine (issue #7373): the store types the variable from the value it is handed, so
 * a {@code Double} there is what every reader downstream renders as {@code "33.0"} - the locator an
 * Inbox row's subject card then cannot load the record through.
 */
class ProcessVariablesEngineTest {

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

    /** Exactly what a generated trigger hands Process.start - Json.stringify of its locator map. */
    private static final String START_PAYLOAD = """
            {"Id": 33, "__entityUrl": "/services/java/billing/gen/billing/api/sales_invoice/SalesInvoiceController",
             "__entityId": 33, "__subjectFields": "Number:text,Total:number"}
            """;

    private static ProcessEngine engine;

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:process-variables-test;DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();
        engine.getRepositoryService()
              .createDeployment()
              .addString("approve.bpmn20.xml", PROCESS_XML)
              .deploy();
        engine.getRuntimeService()
              .startProcessInstanceByKey("approve", ProcessVariables.fromJson(START_PAYLOAD));
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void theRecordsKeyIsReadableAsAnIntegerPathParameter() {
        Task task = engine.getTaskService()
                          .createTaskQuery()
                          .taskAssignee("admin")
                          .includeProcessVariables()
                          .singleResult();

        assertEquals(33L, task.getProcessVariables()
                              .get("__entityId"));
        assertEquals("33", TaskSubject.from(task.getProcessVariables())
                                      .id());
    }
}
