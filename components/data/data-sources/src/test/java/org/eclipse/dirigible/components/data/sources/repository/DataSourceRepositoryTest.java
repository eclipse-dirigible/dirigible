/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
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
 * The Class DataSourceRepositoryTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DataSourceRepositoryTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

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

        // create test Datasources
        createDataSource(datasourceRepository, "/a/b/c/ds1.datasource", "ds1", "description", "");
        createDataSource(datasourceRepository, "/a/b/c/ds2.datasource", "ds2", "description", "");
        createDataSource(datasourceRepository, "/a/b/c/ds3.datasource", "ds3", "description", "");
        createDataSource(datasourceRepository, "/a/b/c/ds4.datasource", "ds4", "description", "");
        createDataSource(datasourceRepository, "/a/b/c/ds5.datasource", "ds5", "description", "");
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        // delete test DataSources
        datasourceRepository.deleteAll();
    }


    /**
     * Gets the one.
     *
     * @return the one
     */
    @Test
    public void getOne() {
        Long id = datasourceRepository.findAll()
                                      .get(0)
                                      .getId();
        Optional<DataSource> optional = datasourceRepository.findById(id);
        DataSource datasource = optional.isPresent() ? optional.get() : null;
        assertNotNull(datasource);
        assertNotNull(datasource.getLocation());
        assertNotNull(datasource.getCreatedBy());
        assertEquals("SYSTEM", datasource.getCreatedBy());
        assertNotNull(datasource.getCreatedAt());
        assertNotNull(datasource.getProperties());
        assertNotNull(datasource.getProperties()
                                .get(0));
        assertEquals(datasource.getName() + "_1", datasource.getProperties()
                                                            .get(0)
                                                            .getName());
    }

    /**
     * Gets the reference using entity manager.
     *
     * @return the reference using entity manager
     */
    @Test
    public void getReferenceUsingEntityManager() {
        Long id = datasourceRepository.findAll()
                                      .get(0)
                                      .getId();
        DataSource datasource = entityManager.getReference(DataSource.class, id);
        assertNotNull(datasource);
        assertNotNull(datasource.getLocation());
    }

    /**
     * Creates the datasource.
     *
     * @param datasourceRepository the datasource repository
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @return the datasource
     */
    public static DataSource createDataSource(DataSourceRepository datasourceRepository, String location, String name, String description,
            String dependencies) {
        DataSource datasource = new DataSource(location, name, description, dependencies, "driver", "url", "");
        datasource.addProperty(name + "_1", "v1");
        datasource.addProperty(name + "_2", "v2");
        datasourceRepository.save(datasource);
        return datasource;
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
