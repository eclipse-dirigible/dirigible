/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import java.io.UncheckedIOException;
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
        JarURLConnection jarUrlConnection = (JarURLConnection) urlConnection;
        // A cached JarURLConnection hands out the JVM-wide shared JarFile - the very instance the
        // application class loader reads resources from. Closing it breaks every read of that JAR
        // that is in flight elsewhere: the JDT.LS installer, which streams a ~50 MB tar.gz out of
        // its own JAR on a background thread while this expansion runs, died with "ZipFile closed".
        // Opting out of the cache yields a handle this method exclusively owns and may close.
        jarUrlConnection.setUseCaches(false);
        try (JarFile jar = jarUrlConnection.getJarFile()) {
            copyRegistryContent(root, jar);
        }
    }

    /**
     * Lays a single jar's {@code META-INF/dirigible/**} payload into the registry - the per-jar
     * counterpart of the startup sweep, used when a module jar joins the running system without a
     * restart. The {@code .skip} marker is honored exactly as at startup.
     *
     * @param jarPath the jar to expand
     */
    public void expand(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            copyRegistryContent("dirigible", jar);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to expand the registry content of [" + jarPath + "]", e);
        }
    }

    /**
     * Removes a project's content from the registry - the inverse of {@link #expand(Path)}, used when
     * the module jar carrying the project leaves the running system. The per-artefact synchronizers
     * clean up the runtime state of the removed files on their next pass.
     *
     * @param project the project name directly under the registry root
     */
    public void remove(String project) {
        String path = IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + project;
        if (repository.hasCollection(path)) {
            repository.removeCollection(path);
            LOGGER.info("Removed the registry content of project [{}]", project);
        }
    }

    /**
     * Copies every entry under {@code META-INF/<root>/} into the registry, honoring the {@code .skip}
     * marker.
     *
     * @param root the root under META-INF, e.g. dirigible
     * @param jar the open jar
     * @throws IOException on a read failure
     */
    private void copyRegistryContent(String root, JarFile jar) throws IOException {
        String jarRoot = "META-INF/" + root;
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
                    String registryPath = entry.getName()
                                               .substring(jarRoot.length());
                    if (!isSafeRegistryPath(registryPath)) {
                        LOGGER.warn("Skipping the jar entry [{}] - its path would escape the registry root", entry.getName()
                                                                                                                  .replaceAll("[\\r\\n]",
                                                                                                                          "_"));
                        continue;
                    }
                    byte[] content = IOUtils.toByteArray(jar.getInputStream(entry));
                    repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + registryPath, content);
                }
            }
        }
    }

    /**
     * Guards against Zip Slip: an archive entry may only map to a path strictly below the registry root
     * - no '.' or '..' segments, no blank segments, no backslashes.
     *
     * @param registryPath the entry's registry-relative path
     * @return true when the path is safe to create
     */
    private static boolean isSafeRegistryPath(String registryPath) {
        if (registryPath.indexOf('\\') >= 0) {
            return false;
        }
        String[] segments = registryPath.split("/");
        boolean hasContent = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (i == 0 && segment.isEmpty()) {
                continue; // the leading separator
            }
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
            hasContent = true;
        }
        return hasContent;
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
