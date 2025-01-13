/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.synchronizer;

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
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

import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class JobSynchronizerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobSynchronizerTest {

    /** The job repository. */
    @Autowired
    private JobRepository jobRepository;

    /** The job synchronizer. */
    @Autowired
    private JobSynchronizer jobSynchronizer;

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

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
        // delete test Tables
        jobRepository.deleteAll();
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
     * @return the job
     */
    public static Job createJob(JobRepository jobRepository, String name, String group, String clazz, String handler, String engine,
            String description, String expression, boolean singleton, List<JobParameter> parameters, String location,
            Set<String> dependencies) {
        Job job = new Job(name, group, clazz, handler, engine, description, expression, singleton, parameters, location, dependencies);
        jobRepository.save(job);
        return job;
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedPath() {
        assertTrue(jobSynchronizer.isAccepted(Path.of("/a/b/c/j1.job"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedArtefact() {
        Job job = createJob(jobRepository, "job", "test_group1", "org....", "test-handler.js", "engine1", "description", "0/1 * * * * ?",
                false, Collections.emptyList(), "/a/b/c/job.job", null);
        assertTrue(jobSynchronizer.isAccepted(job.getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void load() throws ParseException {
        String content =
                "{\"expression\":\"0/1 * * * * ?\",\"group\":\"defined\",\"handler\":\"test/handler.js\",\"description\":\"Control Job\",\"createdBy\":\"system\",\"createdAt\":\"2017-07-06T2:53:01+0000\"}";
        List<Job> list = jobSynchronizer.parse("/test/control.job", content.getBytes());
        assertNotNull(list);
        assertEquals("/test/control.job", list.get(0)
                                              .getLocation());
    }
}
