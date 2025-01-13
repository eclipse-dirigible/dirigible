/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.service.task;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfoQuery;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Component;
import java.util.List;

import static org.eclipse.dirigible.components.engine.bpm.flowable.service.task.TaskQueryExecutor.Type.*;

/**
 * The TaskQueryExecutor.
 */
@Component
public record TaskQueryExecutor(BpmService bpmService) {

    /**
     * Find tasks by process instance id.
     *
     * @param processInstanceId the process instance id
     * @param type the type
     * @return the list
     */
    public List<Task> findTasks(String processInstanceId, Type type) {
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        taskQuery.processInstanceId(processInstanceId);
        return taskQuery.list();
    }

    /**
     * Find tasks.
     *
     * @param type the type
     * @return the list
     */
    public List<Task> findTasks(Type type) {
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        return taskQuery.list();
    }

    private TaskInfoQuery<TaskQuery, Task> prepareQuery(Type type) {
        if (CANDIDATE_GROUPS.equals(type)) {
            return getTaskService().createTaskQuery()
                                   .taskCandidateGroupIn(UserFacade.getUserRoles());
        } else if (ASSIGNEE.equals(type)) {
            return getTaskService().createTaskQuery()
                                   .taskAssignee(UserFacade.getName());
        } else {
            throw new IllegalArgumentException("Unrecognised principal type: " + type);
        }
    }

    /**
     * Gets the task service.
     *
     * @return the task service
     */
    private TaskService getTaskService() {
        return bpmService.getBpmProviderFlowable()
                         .getProcessEngine()
                         .getTaskService();
    }

    /**
     * The Enum Type.
     */
    public enum Type {

        /** The assignee. */
        ASSIGNEE("assignee"),
        /** The candidate groups. */
        CANDIDATE_GROUPS("groups");

        /** The type. */
        private final String type;

        /**
         * Instantiates a new type.
         *
         * @param type the type
         */
        Type(String type) {
            this.type = type;
        }

        /**
         * From string.
         *
         * @param type the type
         * @return the type
         */
        public static Type fromString(String type) {
            for (Type enumValue : values()) {
                if (enumValue.type.equals(type)) {
                    return enumValue;
                }
            }
            throw new IllegalArgumentException("Unknown enum type: " + type);
        }
    }
}
