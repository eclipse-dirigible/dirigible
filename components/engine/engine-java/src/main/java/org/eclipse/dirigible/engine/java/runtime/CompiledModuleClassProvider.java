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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
 * listed class through the <em>application</em> classloader - the same one that already holds the
 * compiled module jar. It runs once, on {@link ApplicationReadyEvent}; a later registry rebuild
 * unions its classes back in (see {@link JavaLoader}).
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

    @Autowired
    public CompiledModuleClassProvider(JavaLoader javaLoader) {
        this.javaLoader = javaLoader;
    }

    /** Discover and register the compiled modules once the application context is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerCompiledModules() {
        List<LoadedClass> classes = discover();
        if (classes.isEmpty()) {
            return;
        }
        javaLoader.installCompiledModules(classes);
        LOGGER.info("Registered [{}] class(es) from AOT compiled module(s) on the classpath", classes.size());
    }

    /**
     * Scan the classpath for {@code .compiled} markers and load every listed class through the
     * application classloader. Package-visible for testing. Never throws: an unreadable marker or an
     * unloadable class is logged and skipped so one bad module cannot block the rest.
     */
    List<LoadedClass> discover() {
        List<LoadedClass> result = new ArrayList<>();
        ClassLoader classLoader = getClass().getClassLoader();
        Resource[] markers;
        try {
            markers = new PathMatchingResourcePatternResolver(classLoader).getResources(MARKER_PATTERN);
        } catch (IOException e) {
            LOGGER.error("Failed to scan the classpath for AOT compiled-module markers", e);
            return result;
        }
        for (Resource marker : markers) {
            String project = projectOf(marker);
            for (String fqn : readClassNames(marker)) {
                try {
                    Class<?> type = Class.forName(fqn, true, classLoader);
                    result.add(new LoadedClass(project, fqn, type, classLoader));
                } catch (ClassNotFoundException | LinkageError e) {
                    LOGGER.error("Compiled-module class [{}] (project [{}]) could not be loaded: {}", fqn, project, e.getMessage(), e);
                }
            }
        }
        return result;
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
    private static List<String> readClassNames(Resource marker) {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(marker.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    names.add(trimmed);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read compiled-module marker [{}]: {}", marker, e.getMessage(), e);
        }
        return names;
    }

}
