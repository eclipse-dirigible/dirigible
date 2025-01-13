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

import jakarta.persistence.EntityManager;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
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

import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The Class JobLogRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobLogRepositoryTest {

    /** The Constant TENANT_ID. */
    private static final String TENANT_ID = "5a2cbfde-bae3-42d5-b0fa-0568466160cd";

    /** The entity manager. */
    @Autowired
    EntityManager entityManager;
    /** The job log repository. */
    @Autowired
    private JobLogRepository jobLogRepository;


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
        // create test Tables
        createJobLog(jobLogRepository, "/a/b/c/jobLog1.jobLog", "jobLog1", "description", null, "job1", "test-handler.js",
                new Timestamp(1667667600), 1L, new Timestamp(1667667660), JobStatus.LOGGED, "job logged");
        createJobLog(jobLogRepository, "/a/b/c/jobLog2.jobLog", "jobLog2", "description", null, "job2", "test-handler.js",
                new Timestamp(1667667720), 2L, new Timestamp(1667667780), JobStatus.LOGGED, "job logged");
        createJobLog(jobLogRepository, "/a/b/c/jobLog3.jobLog", "jobLog3", "description", null, "job3", "test-handler.js",
                new Timestamp(1667667840), 3L, new Timestamp(1667667900), JobStatus.LOGGED, "job logged");
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        // delete test Tables
        jobLogRepository.deleteAll();
    }

    /**
     * Creates the job log.
     *
     * @param jobLogRepository the repository
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param jobName the name
     * @param handler the handler
     * @param triggeredAt the triggeredAt
     * @param triggeredId the triggeredId
     * @param finishedAt the finishedAt
     * @param status the status
     * @param message the message
     */
    public static void createJobLog(JobLogRepository jobLogRepository, String location, String name, String description,
            Set<String> dependencies, String jobName, String handler, Timestamp triggeredAt, long triggeredId, Timestamp finishedAt,
            JobStatus status, String message) {
        JobLog jobLog = new JobLog(location, name, description, dependencies, jobName, handler, triggeredAt, triggeredId, finishedAt,
                status, message, TENANT_ID);
        jobLogRepository.save(jobLog);
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = jobLogRepository.findAll()
                                  .get(0)
                                  .getId();
        Optional<JobLog> optional = jobLogRepository.findById(id);
        JobLog jobLog = optional.orElse(null);
        assertNotNull(jobLog);
        assertNotNull(jobLog.getLocation());
        assertNotNull(jobLog.getCreatedBy());
        assertEquals("/a/b/c/jobLog1.jobLog", jobLog.getLocation());
        assertEquals("jobLog1", jobLog.getName());
        assertEquals("description", jobLog.getDescription());
        assertEquals("job1", jobLog.getJobName());
        assertEquals("test-handler.js", jobLog.getHandler());
        assertEquals(1L, jobLog.getTriggeredId());
        assertEquals(JobStatus.LOGGED, jobLog.getStatus());
        assertEquals("job logged", jobLog.getMessage());
        assertEquals("SYSTEM", jobLog.getCreatedBy());
        assertEquals(TENANT_ID, jobLog.getTenantId());
        assertNotNull(jobLog.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = jobLogRepository.findAll()
                                  .get(0)
                                  .getId();
        JobLog extension = entityManager.getReference(JobLog.class, id);
        assertNotNull(extension);
        assertNotNull(extension.getLocation());
    }
}
