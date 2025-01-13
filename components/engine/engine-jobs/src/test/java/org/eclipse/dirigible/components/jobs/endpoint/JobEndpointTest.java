/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
import org.eclipse.dirigible.components.jobs.service.JobService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Class JobEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobEndpointTest {

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;

    /** The job service. */
    @Autowired
    private JobService jobService;

    /** The job repository. */
    @Autowired
    private JobRepository jobRepository;

    /** The test job. */
    private Job testJob;

    /** The mockMvc. */
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
        // create test Job
        createJob(jobService, "job1", "test_group1", "org....", "test-handler.js", "engine1", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j1.job", null);
        createJob(jobService, "job2", "test_group2", "org....", "test-handler.js", "engine2", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j2.job", null);
        createJob(jobService, "job3", "test_group3", "org....", "test-handler.js", "engine3", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j3.job", null);

        Page<Job> job = jobService.getPages(PageRequest.of(0, BaseEndpoint.DEFAULT_PAGE_SIZE));
        assertNotNull(job);
        assertEquals(3L, job.getTotalElements());

        testJob = job.getContent()
                     .get(0);

        entityManager.refresh(testJob);

    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        jobRepository.deleteAll();
    }

    /**
     * Finds all extension points and checks the location of the first one.
     *
     * @throws Exception the exception
     */
    @Test
    public void findAllExtensionPoints() throws Exception {
        mockMvc.perform(get("/services/jobs"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        // .andExpect(jsonPath("$.content[0].location").value("/a/b/c/j1.job"));
    }

    /**
     * Gets all extension points.
     *
     * @return the all extension points
     * @throws Exception the exception
     */
    @Test
    public void getAllExtensionPoints() throws Exception {
        mockMvc.perform(get("/services/jobs/pages"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Enables the job by name.
     *
     * @throws Exception the exception
     */
    @Test
    public void enableJob() throws Exception {
        // TODO
        mockMvc.perform(put("/services/jobs/enable/{name}", testJob.getName()))
               .andDo(print());
        // .andExpect(status().is2xxSuccessful());

    }

    /**
     * Disables the job by name.
     *
     * @throws Exception the exception
     */
    @Test
    public void disableJob() throws Exception {
        // TODO
        mockMvc.perform(put("/services/jobs/disable/{name}", testJob.getName()))
               .andDo(print());
        // .andExpect(status().is2xxSuccessful());

    }

    /**
     * Creates the job.
     *
     * @param jobService the job repository
     * @param name the job name
     * @param group the job group
     * @param clazz the job clazz
     * @param handler the job handler
     * @param engine the job engine
     * @param description the job description
     * @param expression the job expression
     * @param singleton the singleton
     * @param parameters the job parameters
     * @param location the job location
     * @param dependencies the dependencies
     */
    public static void createJob(JobService jobService, String name, String group, String clazz, String handler, String engine,
            String description, String expression, boolean singleton, List<JobParameter> parameters, String location,
            Set<String> dependencies) {
        Job job = new Job(name, group, clazz, handler, engine, description, expression, singleton, parameters, location, dependencies);
        jobService.save(job);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
