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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A task action that loses the race against another request acting on the same task is answered 409
 * "This task has already been completed", not with the engine's optimistic-lock failure as a 500
 * (issue #8104).
 */
class BpmInboxEndpointTaskConflictTest {

    private static final String TASK_ID = "3622";

    private final BpmService bpmService = mock(BpmService.class);

    private final BpmInboxEndpoint endpoint = new BpmInboxEndpoint(bpmService);

    @BeforeEach
    void theCallerMayActOnTheTask() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn(TASK_ID);
        when(bpmService.findTasks(PrincipalType.CANDIDATE_GROUPS)).thenReturn(List.of(task));
        when(bpmService.findTasks(PrincipalType.ASSIGNEE)).thenReturn(List.of());
    }

    @Test
    void aCompletionLosingTheOptimisticLockToAnotherCompletionIsAConflict() {
        doThrow(new FlowableOptimisticLockingException(
                "VariableInstanceEntity[name=action] was updated by another transaction concurrently")).when(bpmService)
                                                                                                       .completeTask(anyString(), any());
        when(bpmService.isTaskActive(TASK_ID)).thenReturn(false);

        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, complete("resolve"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("This task has already been completed", response.getBody());
    }

    @Test
    void aCompletionReachingTheEngineAfterTheTaskWasCompletedIsAConflict() {
        doThrow(new FlowableObjectNotFoundException("Cannot find task with id " + TASK_ID)).when(bpmService)
                                                                                           .completeTask(anyString(), any());
        when(bpmService.isTaskActive(TASK_ID)).thenReturn(false);

        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, complete("resolve"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void aClaimOfATaskCompletedMeanwhileIsAConflict() {
        doThrow(new IllegalArgumentException("Task with id [" + TASK_ID + "] not found")).when(bpmService)
                                                                                         .claimTask(anyString(), any());
        when(bpmService.isTaskActive(TASK_ID)).thenReturn(false);
        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, action("CLAIM", null));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void aFailureOnATaskThatStillExistsStillPropagates() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(bpmService)
                        .completeTask(anyString(), any());
        when(bpmService.isTaskActive(TASK_ID)).thenReturn(true);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> endpoint.executeTaskAction(TASK_ID, complete("resolve")));

        assertSame(failure, thrown);
    }

    @Test
    void aRejectedCompletionIsStillABadRequest() {
        doThrow(new RuntimeException(new ValidationException("A ticket needs a description"))).when(bpmService)
                                                                                              .completeTask(anyString(), any());
        when(bpmService.isTaskActive(TASK_ID)).thenReturn(true);

        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, complete("resolve"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("A ticket needs a description", response.getBody());
    }

    private static TaskActionData complete(String chosen) {
        return action("COMPLETE", Map.of("action", chosen));
    }

    private static TaskActionData action(String name, Map<String, Object> variables) {
        TaskActionData data = mock(TaskActionData.class);
        when(data.getAction()).thenReturn(name);
        when(data.getData()).thenReturn(variables);
        return data;
    }
}
