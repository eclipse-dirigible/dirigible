/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
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
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class TaskStateService.
 */
@Service
@Transactional
public class TaskStateService {

    /** The Constant DIRIGIBLE_TRACING_TASK_ENABLED. */
    static final String DIRIGIBLE_TRACING_TASK_ENABLED = "DIRIGIBLE_TRACING_TASK_ENABLED";

    /** The repository. */
    private TaskStateRepository repository;

    /**
     * Instantiates a new task state service.
     *
     * @param repository the repository
     */
    public TaskStateService(TaskStateRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    protected TaskStateRepository getRepository() {
        return repository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    public List<TaskState> getAll() {
        return getRepository().findAll();
    }

    /**
     * Gets the pages.
     *
     * @param pageable the pageable
     * @return the pages
     */
    public Page<TaskState> getPages(Pageable pageable) {
        return getRepository().findAll(pageable);
    }

    /**
     * Save.
     *
     * @param taskState the taskState
     * @return the taskState
     */
    private TaskState save(TaskState taskState) {
        return getRepository().saveAndFlush(taskState);
    }

    /**
     * Delete.
     *
     * @param taskState the taskState
     */
    public void delete(TaskState taskState) {
        getRepository().delete(taskState);
    }

    /**
     * Delete all.
     */
    public void deleteAll() {
        getRepository().deleteAll();
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the taskState
     */
    public TaskState findById(Long id) {
        TaskState taskState = getRepository().findById(id)
                                             .orElseThrow(() -> new IllegalArgumentException(
                                                     this.getClass() + ": missing task state with [" + id + "]"));

        if (!TaskStatus.STARTED.equals(taskState.getStatus())) {
            Javers javers = JaversBuilder.javers()
                                         .build();
            Diff diff = javers.compare(taskState.getInput(), taskState.getOutput());
            // TODO to be extracted and printed even prettier
            taskState.setDiff(diff.prettyPrint());
        }

        return taskState;
    }

    /**
     * Find by name.
     *
     * @param execution the execution
     * @return the taskState
     */
    public List<TaskState> findByExecution(String execution) {
        TaskState example = new TaskState();
        example.setExecution(execution);
        return getRepository().findAll(Example.of(example));
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
    public TaskState taskStarted(TaskType taskType, String execution, String step, Map<String, String> input) {
        if (!isTracingEnabled()) {
            return null;
        }
        TaskState taskState = new TaskState();
        taskState.setType(taskType);
        taskState.setExecution(execution != null ? execution : "NONE");
        taskState.setStep(step != null ? step : "NONE");
        taskState.setThread(Thread.currentThread()
                                  .getId()
                + "");
        taskState.setStatus(TaskStatus.STARTED);
        taskState.setStarted(Timestamp.from(Instant.now()));
        if (input != null) {
            taskState.getInput()
                     .putAll(input);
        }
        taskState = save(taskState);
        return taskState;
    }

    /**
     * Task successful.
     *
     * @param taskState the task state
     * @param output the output
     */
    public void taskSuccessful(TaskState taskState, Map<String, String> output) {
        if (!isTracingEnabled()) {
            return;
        }
        if (TaskStatus.STARTED.equals(taskState.getStatus())) {
            taskState.setStatus(TaskStatus.SUCCESSFUL);
            taskState.setEnded(Timestamp.from(Instant.now()));
            if (output != null) {
                taskState.getOutput()
                         .putAll(output);
            }
            taskState = save(taskState);
        } else {
            throw new IllegalArgumentException("Task State must be in status STARTED to be finished successfully");
        }
    }

    /**
     * Task failed.
     *
     * @param taskState the task state
     * @param output the output
     * @param error the error
     */
    public void taskFailed(TaskState taskState, Map<String, String> output, String error) {
        if (!isTracingEnabled()) {
            return;
        }
        if (TaskStatus.STARTED.equals(taskState.getStatus())) {
            taskState.setStatus(TaskStatus.FAILED);
            taskState.setEnded(Timestamp.from(Instant.now()));
            if (output != null) {
                taskState.getOutput()
                         .putAll(output);
            }
            taskState.setError(error != null ? error : "NONE");
            taskState = save(taskState);
        } else {
            throw new IllegalArgumentException("Task State must be in status STARTED to be finished as failed");
        }
    }

    /**
     * Checks if is tracing enabled.
     *
     * @return true, if is tracing enabled
     */
    public boolean isTracingEnabled() {
        return Boolean.parseBoolean(Configuration.get(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "false"));
    }

    /**
     * Enable tracing.
     */
    public void enableTracing() {
        Configuration.set(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "true");
    }

    /**
     * Disable tracing.
     */
    public void disableTracing() {
        Configuration.set(TaskStateService.DIRIGIBLE_TRACING_TASK_ENABLED, "true");
    }

}
