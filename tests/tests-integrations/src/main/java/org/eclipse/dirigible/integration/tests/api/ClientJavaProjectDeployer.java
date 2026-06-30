/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.ProjectUtil;

/**
 * Deploys a client-Java fixture project from {@code src/main/resources/<folder>} straight into the
 * registry and triggers a synchronization cycle — the fast, HTTP-only counterpart to publishing
 * through the IDE. Keeps the fixture {@code .java} sources in real files instead of inlined
 * strings.
 */
final class ClientJavaProjectDeployer {

    private ClientJavaProjectDeployer() {}

    /**
     * Copies the given resources folder into {@code /registry/public/<registryProject>} and forces the
     * synchronizers to run, so the deployed client classes are compiled and wired synchronously.
     */
    static void deploy(IRepository repository, ProjectUtil projectUtil, SynchronizationProcessor synchronizationProcessor,
            String resourcesFolder, String registryProject) {
        Path temp;
        try {
            temp = Files.createTempDirectory("client-java-it-");
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to create temp directory for fixture [" + resourcesFolder + "]", ex);
        }
        try {
            projectUtil.copyResourceFolder(resourcesFolder, temp.toString(), Collections.emptyMap());
            String base = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + registryProject;
            try (Stream<Path> files = Files.walk(temp)) {
                files.filter(Files::isRegularFile)
                     .forEach(file -> {
                         String relative = temp.relativize(file)
                                               .toString()
                                               .replace('\\', '/');
                         repository.createResource(base + "/" + relative, readBytes(file));
                     });
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to walk fixture [" + resourcesFolder + "]", ex);
            }
            synchronizationProcessor.forceProcessSynchronizers();
        } finally {
            try {
                FileUtils.deleteDirectory(temp.toFile());
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to delete temp directory [" + temp + "]", ex);
            }
        }
    }

    private static byte[] readBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read fixture file [" + file + "]", ex);
        }
    }
}
