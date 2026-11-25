/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.SynchronousSpringEventsConfig;
import org.eclipse.dirigible.components.repository.RepositoryConfig;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * A definition left PARSED without its artefact row - two instances synchronizing one SystemDB, a
 * crash between the two writes - gets the artefact back on the next pass, without a restart and
 * without a change to the file (#8873).
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SynchronousSpringEventsConfig.class}, loader = AnnotationConfigContextLoader.class,
        initializers = IsolatedSynchronizationStore.class)
@EntityScan("org.eclipse.dirigible.components")
public class SynchronizationInitializerParsedWithoutArtefactTest {

    private static final String LOCATION = "/test_parsed/healed.extension";

    @Configuration
    @ComponentScan("org.eclipse.dirigible.components")
    static class ContextConfiguration {

        @Bean("SynchronizationInitializerParsedWithoutArtefactTestRepository")
        public IRepository repository() {
            return new RepositoryConfig().repository();
        }

    }

    @Autowired
    private SynchronizationInitializer initializer;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SynchronizationWatcher synchronizationWatcher;

    @Autowired
    private IRepository repository;

    @Autowired
    private DataSource datasource;

    @Test
    public void aParsedDefinitionWithoutItsArtefactRowIsProcessedAgain() throws SQLException {
        initializer.onApplicationEvent(null);
        repository.createResource("/registry/public" + LOCATION,
                ("{\"location\":\"" + LOCATION + "\",\"module\":\"/test_parsed/healed\",\"extensionPoint\":\"/test/test\","
                        + "\"description\":\"Healed Extension\"}").getBytes(StandardCharsets.UTF_8));
        synchronize();

        try (Connection connection = datasource.getConnection()) {
            assertEquals("CREATED", artefactStatus(connection));

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = ?")) {
                delete.setString(1, LOCATION);
                assertEquals(1, delete.executeUpdate());
            }
            assertEquals("PARSED", definitionState(connection));

            synchronize();

            assertEquals("CREATED", artefactStatus(connection));
            assertEquals("PARSED", definitionState(connection));
        }
    }

    private void synchronize() {
        synchronizationWatcher.force();
        synchronizationProcessor.processSynchronizers();
    }

    private static String artefactStatus(Connection connection) throws SQLException {
        return single(connection, "SELECT ARTEFACT_STATUS FROM DIRIGIBLE_EXTENSIONS WHERE ARTEFACT_LOCATION = ?");
    }

    private static String definitionState(Connection connection) throws SQLException {
        return single(connection, "SELECT DEFINITION_STATE FROM DIRIGIBLE_DEFINITIONS WHERE DEFINITION_LOCATION = ?");
    }

    private static String single(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, LOCATION);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next(), "No row for " + LOCATION + ": " + sql);
                String value = rs.getString(1);
                assertFalse(rs.next(), "More than one row for " + LOCATION + ": " + sql);
                return value;
            }
        }
    }

    @SpringBootApplication
    static class TestConfiguration {
    }

}
