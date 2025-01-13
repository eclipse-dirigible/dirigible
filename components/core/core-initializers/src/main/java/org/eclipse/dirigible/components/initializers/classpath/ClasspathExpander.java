/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.classpath;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The Class ClasspathExpander.
 */
@Component
public class ClasspathExpander {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathExpander.class);
    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ClasspathExpander.class);

    /** The repository. */
    private final IRepository repository;

    /**
     * Instantiates a new classpath expander.
     *
     * @param repository the repository
     */
    @Autowired
    public ClasspathExpander(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Expand content.
     */
    public void expandContent() {
        expandContent("dirigible");
        // expandContent("resources" + File.separator + "webjars");
    }

    /**
     * Expand content.
     *
     * @param root the root
     */
    private void expandContent(String root) {
        long startedAtMillis = System.currentTimeMillis();
        LOGGER.info("Expanding the content of [{}]...", root);
        try {
            Enumeration<URL> urls = ClasspathExpander.class.getClassLoader()
                                                           .getResources("META-INF");

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        handleJarURLConnection(root, urlConnection);
                    } else {
                        Path dirPath = Path.of(url.toURI())
                                           .resolve(root);
                        handleLocalDirectory(dirPath);
                    }
                } catch (URISyntaxException | IOException e) {
                    logDirectoryExpandingError(url.toString(), e);
                }
            }
            long elapsedMillis = System.currentTimeMillis() - startedAtMillis;
            LOGGER.info("The content of [{}] has been expanded. It took [{}] millis", root, elapsedMillis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to expand content for " + root, e);
        }
    }

    /**
     * Handle jar URL connection.
     *
     * @param root the root
     * @param urlConnection the url connection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void handleJarURLConnection(String root, URLConnection urlConnection) throws IOException {
        String jarRoot = "META-INF/" + root;
        JarURLConnection jarUrlConnection = (JarURLConnection) urlConnection;
        try (JarFile jar = jarUrlConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            JarEntry maybeSkip = jar.getJarEntry("META-INF/dirigible/.skip");
            if (maybeSkip != null) {
                return;
            }
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName()
                         .startsWith(jarRoot)) {
                    if (!entry.isDirectory()) {
                        byte[] content = IOUtils.toByteArray(jar.getInputStream(entry));
                        String registryPath = entry.getName()
                                                   .substring(jarRoot.length());
                        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + registryPath,
                                content);
                    }
                }
            }
        }
    }

    /**
     * Handle local directory.
     *
     * @param dirPath the dir path
     */
    private void handleLocalDirectory(Path dirPath) {
        try {
            File maybeDir = dirPath.toFile();
            if (!maybeDir.exists() || maybeDir.isFile()) {
                return;
            }
            String registryPath = repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
            FileUtils.copyDirectory(maybeDir, new File(registryPath));
        } catch (IOException e) {
            logDirectoryExpandingError(dirPath.toString(), e);
        }
    }

    /**
     * Log directory expanding error.
     *
     * @param dirPath the dir path
     * @param e the e
     */
    private void logDirectoryExpandingError(String dirPath, Exception e) {
        logger.error("Could not collect dir '" + dirPath + "'", e);
    }
}
