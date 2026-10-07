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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskDTO;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What ONE Inbox listing costs (issue #7232). The store polls the listing every 30 seconds and on
 * every route change, so a read that looks cheap per row is the whole load: #7175 moved the subject
 * variables onto the task query and documented the listing as "one statement", while every row
 * still read its process instance and its identity links - and the instance read is two statements
 * of its own, the query plus its activity ids.
 */
class BpmInboxEndpointQueryCountTest {

    private final BpmService bpmService = mock(BpmService.class);

    private final BpmInboxEndpoint endpoint = new BpmInboxEndpoint(bpmService);

    @BeforeEach
    void noLabels() {
        when(bpmService.getProcessLabelKeys(anyString())).thenReturn(Optional.empty());
        when(bpmService.getListedTaskIdentityLinks(anyString())).thenReturn(List.of());
        when(bpmService.getProcessInstanceById(anyString())).thenReturn(new ProcessInstanceData());
    }

    /** A task of the given process instance - the shape a flow with several open tasks produces. */
    private Task task(String id, String instanceId) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getProcessInstanceId()).thenReturn(instanceId);
        when(task.getProcessVariables()).thenReturn(Map.of());
        return task;
    }

    /**
     * The read every row used to make is made once per INSTANCE. An inbox is a handful of processes
     * with several tasks each, which is why this was the larger half of the per-row cost.
     */
    @Test
    void theProcessInstanceIsReadOncePerInstanceNotOncePerTask() {
        List<Task> found = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            found.add(task("t" + i, "instance-" + (i % 2)));
        }
        when(bpmService.findTasksWithProcessVariables(PrincipalType.ASSIGNEE)).thenReturn(found);

        List<TaskDTO> tasks = endpoint.getTasks("assignee")
                                      .getBody();

        assertEquals(20, tasks.size(), "every task is still listed");
        verify(bpmService, times(1)).getProcessInstanceById("instance-0");
        verify(bpmService, times(1)).getProcessInstanceById("instance-1");
    }

    /**
     * ...and the identity links are read WITHOUT the existence check the listing query just made. The
     * validating read stays for a caller holding an id from the outside; a listing that re-validates
     * every row pays a second statement for an answer it already has.
     */
    @Test
    void aListedTasksIdentityLinksAreReadWithoutRevalidatingTheTask() {
        // the tasks are built BEFORE the stubbing: a mock built inside a when(...) argument leaves
        // Mockito mid-stubbing
        List<Task> found = List.of(task("t1", "instance-1"));
        when(bpmService.findTasksWithProcessVariables(PrincipalType.ASSIGNEE)).thenReturn(found);

        endpoint.getTasks("assignee");

        verify(bpmService, times(1)).getListedTaskIdentityLinks("t1");
        verify(bpmService, never()).getTaskIdentityLinks(anyString());
    }

    /** The one-instance listing takes the same path - one instance read for the whole page. */
    @Test
    void aProcessInstanceListingReadsItsInstanceOnce() {
        List<Task> found = List.of(task("t1", "instance-1"), task("t2", "instance-1"), task("t3", "instance-1"));
        when(bpmService.findTasksWithProcessVariables("instance-1", PrincipalType.ASSIGNEE)).thenReturn(found);

        endpoint.getProcessInstanceTasks("instance-1", "assignee");

        verify(bpmService, times(1)).getProcessInstanceById("instance-1");
    }
}
