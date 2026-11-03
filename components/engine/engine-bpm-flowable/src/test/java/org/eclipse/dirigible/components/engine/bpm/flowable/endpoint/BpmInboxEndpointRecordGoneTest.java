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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A task of a record that has been deleted is refused with a 404 rather than executed (issue
 * #8108).
 */
class BpmInboxEndpointRecordGoneTest {

    private static final String TASK_ID = "8009";

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
    void aTaskWhoseRecordWasDeletedIsRefusedAndNotCompleted() {
        when(bpmService.retireIfRecordGone(TASK_ID)).thenReturn(true);

        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, complete());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("The record this task is about no longer exists", response.getBody());
        verify(bpmService, never()).completeTask(anyString(), any());
    }

    @Test
    void aTaskWhoseRecordExistsIsCompleted() {
        when(bpmService.retireIfRecordGone(TASK_ID)).thenReturn(false);

        ResponseEntity<String> response = endpoint.executeTaskAction(TASK_ID, complete());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bpmService).completeTask(TASK_ID, Map.of("action", "confirm"));
    }

    private static TaskActionData complete() {
        TaskActionData data = mock(TaskActionData.class);
        when(data.getAction()).thenReturn("COMPLETE");
        when(data.getData()).thenReturn(Map.of("action", "confirm"));
        return data;
    }
}
