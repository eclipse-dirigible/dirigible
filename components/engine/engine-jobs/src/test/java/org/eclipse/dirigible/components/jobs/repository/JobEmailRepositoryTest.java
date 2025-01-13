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

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.jobs.domain.JobEmail;
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
 * The Class JobEmailRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobEmailRepositoryTest {

    /** The job email repository. */
    @Autowired
    private JobEmailRepository jobEmailRepository;

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
        // create test JobEmails
        createJobEmail(jobEmailRepository, "/a/b/c/jobEmail1.jobEmail", "jobEmail1", "description", null, "job1", "email1");
        createJobEmail(jobEmailRepository, "/a/b/c/jobEmail2.jobEmail", "jobEmail2", "description", null, "job2", "email2");
        createJobEmail(jobEmailRepository, "/a/b/c/jobEmail3.jobEmail", "jobEmail3", "description", null, "job3", "email3");
        createJobEmail(jobEmailRepository, "/a/b/c/jobEmail4.jobEmail", "jobEmail4", "description", null, "job4", "email4");
        createJobEmail(jobEmailRepository, "/a/b/c/jobEmail5.jobEmail", "jobEmail5", "description", null, "job5", "email5");
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test JobEmails
        jobEmailRepository.deleteAll();
    }

    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = jobEmailRepository.findAll()
                                    .get(0)
                                    .getId();
        Optional<JobEmail> optional = jobEmailRepository.findById(id);
        JobEmail jobEmail = optional.isPresent() ? optional.get() : null;
        assertNotNull(jobEmail);
        assertNotNull(jobEmail.getLocation());
        assertNotNull(jobEmail.getCreatedBy());
        assertEquals("/a/b/c/jobEmail1.jobEmail", jobEmail.getLocation());
        assertEquals("jobEmail1", jobEmail.getName());
        assertEquals("description", jobEmail.getDescription());
        assertEquals("job1", jobEmail.getJobName());
        assertEquals("email1", jobEmail.getEmail());
        assertEquals("SYSTEM", jobEmail.getCreatedBy());
        assertNotNull(jobEmail.getCreatedAt());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = jobEmailRepository.findAll()
                                    .get(0)
                                    .getId();
        JobEmail extension = entityManager.getReference(JobEmail.class, id);
        assertNotNull(extension);
        assertNotNull(extension.getLocation());
    }

    /**
     * Creates the job email.
     *
     * @param jobEmailRepository the job email repository
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param jobName the name
     * @param email the email
     */
    public static void createJobEmail(JobEmailRepository jobEmailRepository, String location, String name, String description,
            Set<String> dependencies, String jobName, String email) {
        JobEmail jobEmail = new JobEmail(location, name, description, dependencies, jobName, email);
        jobEmailRepository.save(jobEmail);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
