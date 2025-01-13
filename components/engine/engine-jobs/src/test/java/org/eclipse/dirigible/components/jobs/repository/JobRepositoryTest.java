/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class JobRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobRepositoryTest {

    /** The job repository. */
    @Autowired
    private JobRepository jobRepository;

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {
        cleanup();
        // create test Job
        createJob(jobRepository, "job1", "test_group1", "org....", "test-handler.js", "engine1", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j1.job", null);
        createJob(jobRepository, "job2", "test_group2", "org....", "test-handler.js", "engine2", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j2.job", null);
        createJob(jobRepository, "job3", "test_group3", "org....", "test-handler.js", "engine3", "description", "0/1 * * * * ?", false,
                Collections.emptyList(), "/a/b/c/j3.job", null);
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test Jobs
        jobRepository.deleteAll();
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = jobRepository.findAll()
                               .get(0)
                               .getId();
        Optional<Job> optional = jobRepository.findById(id);
        Job job = optional.isPresent() ? optional.get() : null;
        assertNotNull(job);
        assertNotNull(job.getLocation());
        assertNotNull(job.getCreatedBy());
        assertEquals("job1", job.getName());
        assertEquals("test_group1", job.getGroup());
        assertEquals("org....", job.getClazz());
        assertEquals("test-handler.js", job.getHandler());
        assertEquals("engine1", job.getEngine());
        assertEquals("description", job.getDescription());
        assertEquals("0/1 * * * * ?", job.getExpression());
        assertEquals("/a/b/c/j1.job", job.getLocation());
        assertEquals("job", job.getType());
        assertEquals("SYSTEM", job.getCreatedBy());
        assertNotNull(job.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = jobRepository.findAll()
                               .get(0)
                               .getId();
        Job job = entityManager.getReference(Job.class, id);
        assertNotNull(job);
        assertNotNull(job.getLocation());
    }

    /**
     * Creates the job.
     *
     * @param jobRepository the job repository
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
    public static void createJob(JobRepository jobRepository, String name, String group, String clazz, String handler, String engine,
            String description, String expression, boolean singleton, List<JobParameter> parameters, String location,
            Set<String> dependencies) {
        Job job = new Job(name, group, clazz, handler, engine, description, expression, singleton, parameters, location, dependencies);
        jobRepository.save(job);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
