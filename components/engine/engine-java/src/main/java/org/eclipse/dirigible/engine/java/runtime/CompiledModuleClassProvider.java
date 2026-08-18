/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Discovers AOT-packaged {@code compiled} modules on the application classpath and registers their
 * already-compiled classes through {@link JavaLoader#installCompiledModules(List)} - <b>without</b>
 * any runtime compilation.
 *
 * <p>
 * A compiled module jar carries a marker at {@code META-INF/dirigible/<project>/.compiled} - a
 * UTF-8 text file listing the module's top-level class binary names (its controllers / repositories
 * / delegates / listeners / …), one per line (blank lines and {@code #} comments ignored). The
 * build emits it; it names exactly the classes the engine should surface to the
 * {@code JavaClassConsumer}s.
 *
 * <p>
 * Discovery reads the markers directly from the classpath (so it does not depend on the
 * {@code ClasspathExpander} having laid the module's registry payload down first) and loads each
 * listed class through the given classloader - by default the current
 * {@link ModulesClassLoaderHolder modules classloader} generation, whose parent chain includes the
 * application classloader holding the {@code /modules} drop-in jars. It runs on
 * {@link ApplicationReadyEvent} and again via {@link #rediscover(ClassLoader)} whenever the
 * dependency layer swaps to a new generation; each pass replaces the compiled set (see
 * {@link JavaLoader#installCompiledModules(List)}).
 */
@Component
public class CompiledModuleClassProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompiledModuleClassProvider.class);

    /**
     * Marker location pattern: one path segment ({@code <project>}) under {@code META-INF/dirigible}.
     */
    private static final String MARKER_PATTERN = "classpath*:META-INF/dirigible/*/.compiled";

    private static final String DIRIGIBLE_ROOT = "META-INF/dirigible/";

    private final JavaLoader javaLoader;
    private final ModulesClassLoaderHolder modulesLoaderHolder;

    @Autowired
    public CompiledModuleClassProvider(JavaLoader javaLoader, ModulesClassLoaderHolder modulesLoaderHolder) {
        this.javaLoader = javaLoader;
        this.modulesLoaderHolder = modulesLoaderHolder;
    }

    /** Discover and register the compiled modules once the application context is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerCompiledModules() {
        rediscover(modulesLoaderHolder.current());
    }

    /**
     * Set once a non-empty compiled set was installed; an empty discovery must still be installed then,
     * so classes of a removed module jar unregister. Before that, an empty discovery is a no-op - the
     * pre-dynamic boot behavior.
     */
    private volatile boolean installedBefore = false;

    /**
     * Re-runs compiled-module discovery through the given classloader and installs the result as the
     * compiled sub-generation - the dependency swap pipeline calls this with the freshly-installed
     * modules classloader so AOT module jars that arrived, changed or left with the dependency change
     * register and unregister without a restart.
     *
     * @param classLoader the classloader to scan and load through
     */
    public synchronized void rediscover(ClassLoader classLoader) {
        List<LoadedClass> classes = discover(classLoader);
        if (classes.isEmpty() && !installedBefore) {
            return;
        }
        installedBefore = !classes.isEmpty();
        javaLoader.installCompiledModules(classes);
        LOGGER.info("Registered [{}] class(es) from AOT compiled module(s) on the classpath", classes.size());
    }

    /**
     * Scan the classpath of the given classloader for {@code .compiled} markers and load every listed
     * class through it. The Spring resource scan covers the application classpath (including the fat
     * jar's nested {@code BOOT-INF/lib} entries); a {@link ModulesClassLoader}'s own jars are
     * additionally scanned entry-by-entry, because a resource scan only sees a jar's directories when
     * the packaging tool emitted directory entries - a guarantee third-party module jars do not give.
     * Package-visible for testing. Never throws: an unreadable marker or an unloadable class is logged
     * and skipped so one bad module cannot block the rest.
     */
    List<LoadedClass> discover(ClassLoader classLoader) {
        Map<String, LoadedClass> result = new LinkedHashMap<>();
        scanWithResolver(classLoader, result);
        if (classLoader instanceof ModulesClassLoader modulesClassLoader) {
            scanModuleJars(modulesClassLoader, result);
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Scan with the Spring pattern resolver.
     *
     * @param classLoader the class loader to scan and load through
     * @param result the discovered classes, keyed by FQN
     */
    private void scanWithResolver(ClassLoader classLoader, Map<String, LoadedClass> result) {
        Resource[] markers;
        try {
            markers = new PathMatchingResourcePatternResolver(classLoader).getResources(MARKER_PATTERN);
        } catch (IOException e) {
            LOGGER.error("Failed to scan the classpath for AOT compiled-module markers", e);
            return;
        }
        for (Resource marker : markers) {
            String project = projectOf(marker);
            List<String> classNames;
            try {
                classNames = readClassNames(marker.getInputStream());
            } catch (IOException e) {
                LOGGER.error("Failed to read compiled-module marker [{}]: {}", marker, e.getMessage(), e);
                continue;
            }
            load(project, classNames, classLoader, result);
        }
    }

    /**
     * Scan the modules classloader's own jars entry-by-entry.
     *
     * @param classLoader the modules class loader
     * @param result the discovered classes, keyed by FQN
     */
    private void scanModuleJars(ModulesClassLoader classLoader, Map<String, LoadedClass> result) {
        for (Path jarPath : classLoader.jars()) {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String project = markerProject(entry.getName());
                    if (project == null) {
                        continue;
                    }
                    List<String> classNames;
                    try (var in = jar.getInputStream(entry)) {
                        classNames = readClassNames(in);
                    }
                    load(project, classNames, classLoader, result);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to scan the module jar [{}] for AOT compiled-module markers: {}", jarPath, e.getMessage(), e);
            }
        }
    }

    /**
     * Load the listed classes through the given classloader. A class discovered by an earlier scan
     * wins.
     *
     * @param project the owning project
     * @param classNames the listed class names
     * @param classLoader the class loader to load through
     * @param result the discovered classes, keyed by FQN
     */
    private void load(String project, List<String> classNames, ClassLoader classLoader, Map<String, LoadedClass> result) {
        for (String fqn : classNames) {
            if (result.containsKey(fqn)) {
                continue;
            }
            try {
                Class<?> type = Class.forName(fqn, true, classLoader);
                result.put(fqn, new LoadedClass(project, fqn, type, type.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.error("Compiled-module class [{}] (project [{}]) could not be loaded: {}", fqn, project, e.getMessage(), e);
            }
        }
    }

    /**
     * The project of a jar entry that is a compiled-module marker, null for any other entry.
     *
     * @param entryName the jar entry name
     * @return the project, or null when the entry is not a marker
     */
    private static String markerProject(String entryName) {
        if (!entryName.startsWith(DIRIGIBLE_ROOT) || !entryName.endsWith("/.compiled")) {
            return null;
        }
        String project = entryName.substring(DIRIGIBLE_ROOT.length(), entryName.length() - "/.compiled".length());
        return project.isEmpty() || project.indexOf('/') >= 0 ? null : project;
    }

    /**
     * The {@code <project>} path segment immediately under {@code META-INF/dirigible/} in the marker
     * URL.
     */
    private static String projectOf(Resource marker) {
        try {
            String url = marker.getURL()
                               .toString();
            int at = url.indexOf(DIRIGIBLE_ROOT);
            if (at < 0) {
                return "";
            }
            String rest = url.substring(at + DIRIGIBLE_ROOT.length());
            int slash = rest.indexOf('/');
            return slash < 0 ? rest : rest.substring(0, slash);
        } catch (IOException e) {
            LOGGER.error("Failed to resolve the project of compiled-module marker [{}]: {}", marker, e.getMessage(), e);
            return "";
        }
    }

    /** Non-blank, non-comment lines of a marker, trimmed. */
    private static List<String> readClassNames(InputStream markerContent) throws IOException {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(markerContent, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    names.add(trimmed);
                }
            }
        }
        return names;
    }

}
