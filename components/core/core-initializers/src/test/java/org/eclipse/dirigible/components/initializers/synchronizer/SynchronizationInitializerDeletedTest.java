/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.SynchronousSpringEventsConfig;
import org.eclipse.dirigible.components.repository.RepositoryConfig;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryWriteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * The Class SynchronizationInitializerTest.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SynchronousSpringEventsConfig.class}, loader = AnnotationConfigContextLoader.class)
@EntityScan("org.eclipse.dirigible.components")
public class SynchronizationInitializerDeletedTest {

    /**
     * The Class ContextConfiguration.
     */
    @Configuration
    @ComponentScan("org.eclipse.dirigible.components")
    static class ContextConfiguration {

        /**
         * Repository.
         *
         * @return the i repository
         */
        @Bean("SynchronizationInitializerDeletedTestReposiotry")
        public IRepository repository() {
            return new RepositoryConfig().repository();
        }

    }

    /** The listener. */
    @Autowired
    private SynchronizationInitializer initializer;

    /** The synchronization processor. */
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /** The synchronization watcher. */
    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    /** The repository. */
    @Autowired
    private IRepository repository;

    /** The datasource. */
    @Autowired
    private DataSource datasource;

    /**
     * Test context started handler.
     *
     * @throws RepositoryWriteException the repository write exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testSynchronizationDeleted() throws RepositoryWriteException, IOException, SQLException {

        try (Connection connection = datasource.getConnection()) {

            // initialization
            initializer.onApplicationEvent(null);

            // check if the definition has been created
            CheckDefinitionUtils.isDefinitionForDeletionExists(connection);

            // check if the artefact has been created
            CheckArtefactUtils.isArtefactForDeletionCreated(connection);

            // delete the artefact
            repository.getResource("/registry/public/test/test_deleted.extension")
                      .delete();

            // process again
            synchronizationWatcher.force();
            synchronizationProcessor.processSynchronizers();

            // check if the definition has been set as deleted
            CheckDefinitionUtils.isDefinitionDeleted(connection);

            // check if the artefact has been deleted
            CheckArtefactUtils.isArtefactDeleted(connection);
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
