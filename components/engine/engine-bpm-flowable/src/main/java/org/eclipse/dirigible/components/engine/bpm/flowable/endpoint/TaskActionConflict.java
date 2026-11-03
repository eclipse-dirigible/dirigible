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

import java.util.function.Supplier;

import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Answers a task action that lost the race against another session acting on the same task with a
 * 409 instead of the engine's failure (issue #8104).
 *
 * <p>
 * Two COMPLETE calls on one task - the ordinary double click - both pass the permission check, and
 * the engine lets only one of them through: the other fails with Flowable's optimistic-lock text
 * ({@code ... was updated by another transaction concurrently}), or, when it reaches the engine a
 * moment later, with the task "not found". Either way the record is consistent and only the answer
 * was wrong - a 500. A claim or unclaim that meets a task completed meanwhile fails the same way.
 *
 * <p>
 * Only a task confirmed gone is answered with the conflict: the losing statement waits for the
 * winning transaction's row lock, so by the time it fails the winner has committed and the task is
 * no longer active. A failure on a task that still exists is a real failure and propagates as
 * before.
 */
final class TaskActionConflict {

    /** The reason the losing action is answered with. */
    static final String ALREADY_COMPLETED = "This task has already been completed";

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskActionConflict.class);

    private TaskActionConflict() {}

    /**
     * Performs a task action, answering 409 when it failed because the task is gone.
     *
     * @param taskId the task acted on
     * @param bpmService the service telling whether the task still exists
     * @param action the action
     * @return the action's answer, or a 409 with {@link #ALREADY_COMPLETED} when the task no longer
     *         exists after the action failed
     */
    static ResponseEntity<String> unlessTaskGone(String taskId, BpmService bpmService, Supplier<ResponseEntity<String>> action) {
        try {
            return action.get();
        } catch (RuntimeException ex) {
            if (bpmService.isTaskActive(taskId)) {
                throw ex;
            }
            LOGGER.debug("Task [{}] was completed by another request while this one acted on it - answering 409", taskId, ex);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(ALREADY_COMPLETED);
        }
    }
}
