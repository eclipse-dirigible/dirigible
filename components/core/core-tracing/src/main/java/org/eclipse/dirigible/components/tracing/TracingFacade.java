/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tracing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class TracingFacade.
 */
@Component
public class TracingFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TracingFacade.class);

    /** The tracing facade. */
    private static TracingFacade INSTANCE;

    /** The task state service. */
    private final TaskStateService taskStateService;

    /**
     * Instantiates a new tracing facade.
     *
     * @param taskStateService the task state service
     */
    @Autowired
    private TracingFacade(TaskStateService taskStateService) {
        this.taskStateService = taskStateService;
    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the tracing facade
     */
    public static TracingFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the task state service.
     *
     * @return the task state service
     */
    public TaskStateService getTaskStateService() {
        return taskStateService;
    }

    /**
     * Task started.
     *
     * @param taskType the task type
     * @param execution the execution
     * @param step the step
     * @param input the input
     * @return the task state
     */
    public static TaskState taskStarted(TaskType taskType, String execution, String step, Map<String, String> input) {
        return get().getTaskStateService()
                    .taskStarted(taskType, execution, step, input);
    }

    /**
     * Task successful.
     *
     * @param taskState the task state
     * @param output the output
     */
    public static void taskSuccessful(TaskState taskState, Map<String, String> output) {
        get().getTaskStateService()
             .taskSuccessful(taskState, output);
    }

    /**
     * Task failed.
     *
     * @param taskState the task state
     * @param output the output
     * @param error the error
     */
    public static void taskFailed(TaskState taskState, Map<String, String> output, String error) {
        get().getTaskStateService()
             .taskFailed(taskState, output, error);
    }

    /**
     * Checks if is tracing enabled.
     *
     * @return true, if is tracing enabled
     */
    public static boolean isTracingEnabled() {
        return Boolean.parseBoolean(Configuration.get(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "false"));
    }

    /**
     * Enable tracing.
     */
    public static void enableTracing() {
        Configuration.set(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "true");
    }

    /**
     * Disable tracing.
     */
    public static void disableTracing() {
        Configuration.set(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "true");
    }


}
