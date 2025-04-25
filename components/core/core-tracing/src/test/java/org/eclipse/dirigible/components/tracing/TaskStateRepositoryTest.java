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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Timestamp;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * The Class TableRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class TaskStateRepositoryTest {

    /** The task state repository. */
    @Autowired
    private TaskStateRepository taskStateRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test task states
        taskStateRepository.deleteAll();
    }

    /**
     * Gets the one.
     *
     * @return the one
     * @throws Exception the exception
     */
    @Test
    public void getOne() throws Exception {

        cleanup();

        TaskState taskState = createSampleTaskState();

        taskState.getInput()
                 .put("var1", "val1");
        taskState.getInput()
                 .put("var2", "val2");

        taskStateRepository.save(taskState);


        Long id = taskStateRepository.findAll()
                                     .get(0)
                                     .getId();
        Optional<TaskState> optional = taskStateRepository.findById(id);
        TaskState result = optional.isPresent() ? optional.get() : null;
        assertNotNull(result);
        assertNotNull(result.getExecution());
        assertNotNull(result.getStep());
        assertEquals(0, result.getType()
                              .ordinal());
        assertNotNull(result.getStarted());
        assertNotNull(result.getStatus());
        assertEquals(0, result.getStatus()
                              .ordinal());
        assertNotNull(result.getInput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));
    }


    /**
     * Creates the sample task state.
     *
     * @return the task state
     */
    public TaskState createSampleTaskState() {
        TaskState taskState = new TaskState();
        taskState.setType(TaskType.BPM);
        taskState.setExecution("exec1");
        taskState.setStep("step1");
        taskState.setStatus(TaskStatus.STARTED);
        taskState.setStarted(Timestamp.valueOf("2025-04-12 12:00:00"));
        return taskState;
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        TaskState taskState = createSampleTaskState();
        taskStateRepository.save(taskState);

        Long id = taskStateRepository.findAll()
                                     .get(0)
                                     .getId();
        TaskState result = entityManager.getReference(TaskState.class, id);
        assertNotNull(result);
        assertNotNull(result.getExecution());
    }

    /**
     * Gets the input output.
     *
     * @return the input output
     * @throws Exception the exception
     */
    @Test
    public void getInputOutput() throws Exception {

        cleanup();

        TaskState taskState = createSampleTaskState();

        taskState.getInput()
                 .put("var1", "val1");
        taskState.getInput()
                 .put("var2", "val2");
        taskState.getOutput()
                 .put("var1", "val1");
        taskState.getOutput()
                 .put("var2", "val2");

        taskStateRepository.save(taskState);

        Long id = taskStateRepository.findAll()
                                     .get(0)
                                     .getId();
        Optional<TaskState> optional = taskStateRepository.findById(id);
        TaskState result = optional.isPresent() ? optional.get() : null;
        assertNotNull(result);
        assertNotNull(result.getInput());
        assertNotNull(result.getOutput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));
        assertEquals("val1", result.getOutput()
                                   .get("var1"));
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
