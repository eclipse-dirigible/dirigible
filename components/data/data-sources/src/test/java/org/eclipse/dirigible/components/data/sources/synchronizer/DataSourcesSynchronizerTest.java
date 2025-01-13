/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
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
import jakarta.persistence.EntityManager;

/**
 * The Class DataSourcesSynchronizerTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DataSourcesSynchronizerTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The datasource synchronizer. */
    @Autowired
    private DataSourcesSynchronizer datasourcesSynchronizer;

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

        // create test DataSources
        datasourceRepository.save(createDataSource("/a/b/c/ds1.datasource", "ds1", "description"));
        datasourceRepository.save(createDataSource("/a/b/c/ds2.datasource", "ds2", "description"));
        datasourceRepository.save(createDataSource("/a/b/c/ds3.datasource", "ds3", "description"));
        datasourceRepository.save(createDataSource("/a/b/c/ds4.datasource", "ds4", "description"));
        datasourceRepository.save(createDataSource("/a/b/c/ds5.datasource", "ds5", "description"));
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        datasourceRepository.deleteAll();
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedPath() {
        assertTrue(datasourcesSynchronizer.isAccepted(Path.of("/a/b/c/ds1.datasource"), null));
    }

    /**
     * Checks if is accepted.
     */
    @Test
    public void isAcceptedArtefact() {
        assertTrue(datasourcesSynchronizer.isAccepted(createDataSource("/a/b/c/ds1.datasource", "ds1", "description").getType()));
    }

    /**
     * Load the artefact.
     *
     * @throws ParseException the parse exception
     */
    @Test
    public void load() throws ParseException {
        String content =
                "{\"location\":\"/test/test.datasource\",\"name\":\"test\",\"driver\":\"org.h2.Driver\",\"url\":\"jdbc:h2:~/test\",\"username\":\"sa\",\"password\":\"\"}";
        List<DataSource> list = datasourcesSynchronizer.parse("/test/test.datasource", content.getBytes());
        assertNotNull(list);
        assertEquals("/test/test.datasource", list.get(0)
                                                  .getLocation());
    }

    /**
     * Creates the datasource.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @return the extension point
     */
    public static DataSource createDataSource(String location, String name, String description) {
        return new DataSource(location, name, description, "", "", "", "");
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
