/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskDTO;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * How an Inbox row gets its subject: off the process variables the LISTING QUERY already fetched
 * with the task, never with a variable query per task on every poll (issue #7141).
 */
class BpmInboxEndpointSubjectTest {

    private final BpmService bpmService = mock(BpmService.class);

    private final BpmInboxEndpoint endpoint = new BpmInboxEndpoint(bpmService);

    private static Map<String, Object> subjectVariables(int id) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("__entityUrl", "/services/java/billing/gen/billing/api/sales_invoice/SalesInvoiceController");
        variables.put("__entityId", id);
        variables.put("__subjectFields", "Number:text,Total:number");
        return variables;
    }

    private Task task(String id, Map<String, Object> processVariables) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getProcessInstanceId()).thenReturn("instance-" + id);
        when(task.getProcessVariables()).thenReturn(processVariables);
        ProcessInstanceData instance = new ProcessInstanceData();
        when(bpmService.getProcessInstanceById("instance-" + id)).thenReturn(instance);
        when(bpmService.getTaskIdentityLinks(id)).thenReturn(List.of());
        return task;
    }

    /**
     * The whole listing costs the one task query - a hundred open tasks used to cost a hundred variable
     * reads, twice per poll, every 30 seconds.
     */
    @Test
    void theSubjectIsReadOffTheTaskNotQueriedPerTask() {
        List<Task> found = List.of(task("t1", subjectVariables(6)), task("t2", subjectVariables(7)));
        when(bpmService.findTasksWithProcessVariables(PrincipalType.ASSIGNEE)).thenReturn(found);
        when(bpmService.getProcessLabelKeys(anyString())).thenReturn(Optional.empty());

        List<TaskDTO> tasks = endpoint.getTasks("assignee")
                                      .getBody();

        assertEquals(2, tasks.size());
        assertNotNull(tasks.get(0)
                           .getSubject());
        assertEquals("7", tasks.get(1)
                               .getSubject()
                               .id());
        verify(bpmService, never()).getTaskVariables(anyString());
        verify(bpmService, never()).findTasks(PrincipalType.ASSIGNEE);
    }

    /** A process seeding no subject variables is still listed - it just has no subject. */
    @Test
    void aTaskWithoutSubjectVariablesIsStillListed() {
        List<Task> found = List.of(task("t1", Map.of()), task("t2", null));
        when(bpmService.findTasksWithProcessVariables(PrincipalType.CANDIDATE_GROUPS)).thenReturn(found);
        when(bpmService.getProcessLabelKeys(anyString())).thenReturn(Optional.empty());

        List<TaskDTO> tasks = endpoint.getTasks("groups")
                                      .getBody();

        assertEquals(2, tasks.size());
        assertNull(tasks.get(0)
                        .getSubject());
        assertNull(tasks.get(1)
                        .getSubject());
        verify(bpmService, never()).getTaskVariables(anyString());
    }

    /** The tasks of one process instance are listed the same way - variables ride the query. */
    @Test
    void aProcessInstanceListingAlsoRidesTheQuery() {
        List<Task> found = List.of(task("t1", subjectVariables(6)));
        when(bpmService.findTasksWithProcessVariables("instance-t1", PrincipalType.ASSIGNEE)).thenReturn(found);
        when(bpmService.getProcessLabelKeys(anyString())).thenReturn(Optional.empty());

        List<TaskDTO> tasks = endpoint.getProcessInstanceTasks("instance-t1", "assignee")
                                      .getBody();

        assertEquals("6", tasks.get(0)
                               .getSubject()
                               .id());
        verify(bpmService, never()).getTaskVariables(anyString());
    }
}
