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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Gives the synchronization tests' context a store of its own: an in-memory SystemDB and a
 * repository in a fresh temporary folder, both empty when the context starts. The tests assert a
 * first synchronization (an artefact CREATED, a definition BROKEN), which the file-backed defaults
 * under {@code target/dirigible} only give on a run that found them absent. Deleting those files
 * instead is not an option: another test class's cached context holds the same SystemDB file open
 * for the rest of the JVM, and Windows refuses to delete an open file.
 *
 * <p>
 * The store is chosen through runtime configuration, which outranks every other source and is read
 * when the data source and repository beans are created. It stays set until the context closes, so
 * a context created after this one in the same JVM gets the same store.
 */
class IsolatedSynchronizationStore implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String SYSTEM_DB_URL = "DIRIGIBLE_DATABASE_SYSTEM_URL";

    private static final String REPOSITORY_ROOT_FOLDER = "DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Path repositoryRoot = createRepositoryRoot();
        Configuration.set(SYSTEM_DB_URL, "jdbc:h2:mem:SystemDB-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        Configuration.set(REPOSITORY_ROOT_FOLDER, repositoryRoot.toString());
        context.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent) {
                Configuration.remove(SYSTEM_DB_URL);
                Configuration.remove(REPOSITORY_ROOT_FOLDER);
                FileUtils.deleteQuietly(repositoryRoot.toFile());
            }
        });
    }

    private static Path createRepositoryRoot() {
        try {
            return Files.createTempDirectory("dirigible-synchronization-test");
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create the test repository folder", ex);
        }
    }

}
