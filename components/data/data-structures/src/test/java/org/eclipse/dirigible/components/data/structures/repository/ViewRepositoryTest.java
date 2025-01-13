/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.data.structures.domain.View;
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
 * The Class ViewRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class ViewRepositoryTest {

    /** The view repository. */
    @Autowired
    private ViewRepository viewRepository;

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

        // create test Views
        createView(viewRepository, "/a/b/c/v1.view", "v1", "description", null);
        createView(viewRepository, "/a/b/c/v2.view", "v2", "description", null);
        createView(viewRepository, "/a/b/c/v3.view", "v3", "description", null);
        createView(viewRepository, "/a/b/c/v4.view", "v4", "description", null);
        createView(viewRepository, "/a/b/c/v5.view", "v5", "description", null);
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test Views
        viewRepository.deleteAll();
    }


    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = viewRepository.findAll()
                                .get(0)
                                .getId();
        Optional<View> optional = viewRepository.findById(id);
        View view = optional.isPresent() ? optional.get() : null;
        assertNotNull(view);
        assertNotNull(view.getLocation());
        assertNotNull(view.getCreatedBy());
        assertEquals("SYSTEM", view.getCreatedBy());
        assertNotNull(view.getCreatedAt());
        assertNotNull(view.getQuery());
        // assertEquals("view:/a/b/c/t1.view:t1", view.getKey());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = viewRepository.findAll()
                                .get(0)
                                .getId();
        View view = entityManager.getReference(View.class, id);
        assertNotNull(view);
        assertNotNull(view.getLocation());
    }

    /**
     * Creates the view.
     *
     * @param viewRepository the view repository
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @return the view
     */
    public static View createView(ViewRepository viewRepository, String location, String name, String description,
            Set<String> dependencies) {
        View view = new View(location, name, description, dependencies, null, "VIEW", "SELECT ...");
        viewRepository.save(view);
        return view;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
