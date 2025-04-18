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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;

/**
 * The Class TaskStateEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class TaskStateEndpointTest {

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;

    /** The tracing service. */
    @Autowired
    private TaskStateService taskStateService;

    /** The tracing repository. */
    @Autowired
    private TaskStateRepository taskStateRepository;

    /** The test task state. */
    private TaskState testTaskState;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        Map<String, String> vars = new TreeMap<String, String>();
        vars.put("n1", "v1");
        // create test task states
        TaskState ts1 = taskStateService.taskStarted(TaskType.BPM, "exec1", "step1", null);
        taskStateService.taskStarted(TaskType.BPM, "exec1", "step2", vars);
        taskStateService.taskStarted(TaskType.BPM, "exec2", "step1", vars);
        taskStateService.taskStarted(TaskType.BPM, "exec2", "step2", vars);
        TaskState ts3 = taskStateService.taskStarted(TaskType.BPM, "exec3", "step1", vars);

        taskStateService.taskSuccessful(ts1, vars);
        taskStateService.taskFailed(ts3, vars, "error1");

        Page<TaskState> list = taskStateService.getPages(PageRequest.of(0, BaseEndpoint.DEFAULT_PAGE_SIZE));
        assertNotNull(list);
        assertEquals(5L, list.getTotalElements());

        testTaskState = list.getContent()
                            .get(0);

        entityManager.refresh(testTaskState);
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        taskStateRepository.deleteAll();
    }

    /**
     * Find all task states.
     */
    @Test
    public void findAllDataSources() {
        Integer size = 10;
        Integer page = 0;
        Pageable pageable = PageRequest.of(page, size);
        assertNotNull(taskStateService.getPages(pageable));
    }

    /**
     * Gets the task state by id.
     *
     * @return the task state by id
     * @throws Exception the exception
     */
    @Test
    public void getTaskStateById() throws Exception {
        Long id = testTaskState.getId();

        mockMvc.perform(get("/services/core/tracing/{id}", id))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the task state by execution.
     *
     * @return the task state by execution
     * @throws Exception the exception
     */
    @Test
    public void getTaskStatesByExecution() throws Exception {
        String execution = testTaskState.getExecution();

        mockMvc.perform(get("/services/core/tracing/search?execution={execution}", execution))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the pages task states.
     *
     * @return the pages task states
     * @throws Exception the exception
     */
    @Test
    public void getPagesTaskStates() throws Exception {

        mockMvc.perform(get("/services/core/tracing/pages"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the all task states.
     *
     * @return the all task states
     * @throws Exception the exception
     */
    @Test
    public void getAllDataSources() throws Exception {
        mockMvc.perform(get("/services/core/tracing"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
