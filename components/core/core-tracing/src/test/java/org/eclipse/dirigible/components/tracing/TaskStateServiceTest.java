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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

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
public class TaskStateServiceTest {

    /** The task state service. */
    @Autowired
    private TaskStateService taskStateService;

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test task states
        taskStateService.deleteAll();
    }

    /**
     * Start finish.
     *
     * @throws Exception the exception
     */
    @Test
    public void startFinish() throws Exception {

        cleanup();

        Map<String, String> input = new TreeMap<String, String>();
        input.put("var1", "val1");
        input.put("var2", "val2");

        Map<String, String> output = new TreeMap<String, String>();
        output.put("var1", "val1_");
        output.put("var2", "val2_");

        TaskState taskState = taskStateService.taskStarted(TaskType.BPM, "exec1", "step1", input);

        Long id = taskStateService.getAll()
                                  .get(0)
                                  .getId();
        TaskState result = taskStateService.findById(id);
        assertNotNull(result);
        assertNotNull(result.getInput());
        assertNotNull(result.getOutput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));

        taskStateService.taskSuccessful(taskState, output);

        assertEquals("val1_", result.getOutput()
                                    .get("var1"));
        assertNull(result.getError());
    }

    /**
     * Start fail.
     *
     * @throws Exception the exception
     */
    @Test
    public void startFail() throws Exception {

        cleanup();

        Map<String, String> input = new TreeMap<String, String>();
        input.put("var1", "val1");
        input.put("var2", "val2");

        Map<String, String> output = new TreeMap<String, String>();
        output.put("var1", "val1_");
        output.put("var2", "val2_");

        TaskState taskState = taskStateService.taskStarted(TaskType.BPM, "exec1", "step1", input);

        Long id = taskStateService.getAll()
                                  .get(0)
                                  .getId();
        TaskState result = taskStateService.findById(id);
        assertNotNull(result);
        assertEquals(TaskType.BPM, result.getType());
        assertEquals("exec1", result.getExecution());
        assertEquals("step1", result.getStep());
        assertNotNull(result.getInput());
        assertNotNull(result.getOutput());
        assertEquals("val1", result.getInput()
                                   .get("var1"));

        taskStateService.taskFailed(taskState, output, "error1");

        assertEquals("val1_", result.getOutput()
                                    .get("var1"));
        assertEquals("error1", result.getError());


    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
