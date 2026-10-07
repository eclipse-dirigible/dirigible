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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
 * A task completed while the Inbox list is being built is omitted, not a 500 for every caller
 * (issue #7575). The listing query and each row's per-task reads are separate statements, so a task
 * another session completes in between is "not found" by the per-task read - and, when it was its
 * process's last task, its process instance is gone too. Only a task confirmed gone is omitted: a
 * read failing for any other reason still fails the list.
 */
class BpmInboxEndpointVanishedTaskTest {

    private final BpmService bpmService = mock(BpmService.class);

    private final BpmInboxEndpoint endpoint = new BpmInboxEndpoint(bpmService);

    @BeforeEach
    void noLabels() {
        when(bpmService.getProcessLabelKeys(anyString())).thenReturn(Optional.empty());
    }

    private Task task(String id) {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(id);
        when(task.getProcessInstanceId()).thenReturn("instance-" + id);
        when(task.getProcessVariables()).thenReturn(Map.of());
        when(bpmService.getProcessInstanceById("instance-" + id)).thenReturn(new ProcessInstanceData());
        when(bpmService.getListedTaskIdentityLinks(id)).thenReturn(List.of());
        when(bpmService.isTaskActive(id)).thenReturn(true);
        return task;
    }

    /** The issue's trace: the per-task read's tenant validator no longer finds the completed task. */
    @Test
    void aTaskCompletedBeforeItsIdentityLinksAreReadIsOmitted() {
        List<Task> found = List.of(task("t1"), task("t2"), task("t3"));
        when(bpmService.getListedTaskIdentityLinks("t2")).thenThrow(
                new IllegalArgumentException("Task with id [t2] not found or does not belong to current tenant"));
        when(bpmService.isTaskActive("t2")).thenReturn(false);
        when(bpmService.findTasksWithProcessVariables(PrincipalType.CANDIDATE_GROUPS)).thenReturn(found);

        List<TaskDTO> tasks = endpoint.getTasks("groups")
                                      .getBody();

        assertEquals(List.of("t1", "t3"), ids(tasks), "the vanished task is omitted, the rest of the list still answers");
    }

    /**
     * Completing a process's last task ends the process: its instance is gone by the time it is read.
     */
    @Test
    void aTaskWhoseProcessEndedBeforeItWasReadIsOmitted() {
        List<Task> found = List.of(task("t1"), task("t2"));
        when(bpmService.getProcessInstanceById("instance-t1")).thenThrow(new NullPointerException("processInstance"));
        when(bpmService.isTaskActive("t1")).thenReturn(false);
        when(bpmService.findTasksWithProcessVariables(PrincipalType.ASSIGNEE)).thenReturn(found);

        List<TaskDTO> tasks = endpoint.getTasks("assignee")
                                      .getBody();

        assertEquals(List.of("t2"), ids(tasks));
    }

    /** A per-task read that fails while the task is still there is a real failure, not the race. */
    @Test
    void aFailureOnATaskThatStillExistsStillFailsTheList() {
        List<Task> found = List.of(task("t1"));
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(bpmService.getListedTaskIdentityLinks("t1")).thenThrow(failure);
        when(bpmService.findTasksWithProcessVariables(PrincipalType.ASSIGNEE)).thenReturn(found);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> endpoint.getTasks("assignee"));

        assertSame(failure, thrown);
    }

    /** The listing of one process instance's tasks takes the same path. */
    @Test
    void aProcessInstanceListingOmitsAVanishedTaskToo() {
        List<Task> found = List.of(task("t1"), task("t2"));
        when(bpmService.getListedTaskIdentityLinks("t1")).thenThrow(
                new IllegalArgumentException("Task with id [t1] not found or does not belong to current tenant"));
        when(bpmService.isTaskActive("t1")).thenReturn(false);
        when(bpmService.findTasksWithProcessVariables("instance", PrincipalType.ASSIGNEE)).thenReturn(found);

        List<TaskDTO> tasks = endpoint.getProcessInstanceTasks("instance", "assignee")
                                      .getBody();

        assertEquals(List.of("t2"), ids(tasks));
    }

    private static List<String> ids(List<TaskDTO> tasks) {
        return tasks.stream()
                    .map(TaskDTO::getId)
                    .toList();
    }
}
