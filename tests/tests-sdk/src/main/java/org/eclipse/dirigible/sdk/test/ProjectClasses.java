/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.dirigible.engine.java.runtime.ClientTestSources;

/**
 * The application classes of a project: every top-level class under {@code gen/} and
 * {@code custom/}, its unit tests under {@code custom/test/} excepted - the same set the platform
 * compiles on publish. The classes themselves come from the test classpath, compiled by the
 * module's build, so a test and the slice share one {@link Class} per type.
 */
final class ProjectClasses {

    private static final List<String> SOURCE_FOLDERS = List.of("gen", "custom");
    private static final String JAVA_EXTENSION = ".java";
    private static final List<String> NOT_CLASSES = List.of("package-info.java", "module-info.java");

    private ProjectClasses() {}

    /**
     * Loads the application classes of a project.
     *
     * @param projectRoot the project folder (the one holding {@code gen/} and {@code custom/})
     * @param loader the class loader the test classes were loaded by
     * @return the classes, in source-path order
     * @throws IllegalStateException if the project has no sources, or a source has no class on the test
     *         classpath
     */
    static List<Class<?>> load(Path projectRoot, ClassLoader loader) {
        List<String> names = classNames(projectRoot);
        if (names.isEmpty()) {
            throw new IllegalStateException("No Java sources under gen/ or custom/ of [" + projectRoot
                    + "] - point @IntentSlice(project = ...) at the project folder, or run the test from it");
        }
        List<Class<?>> classes = new ArrayList<>(names.size());
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            try {
                classes.add(Class.forName(name, false, loader));
            } catch (ClassNotFoundException ex) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("The project's classes are not on the test classpath: " + missing
                    + " - compile the project folder as a test source root, so gen/, custom/ and custom/test/ build together");
        }
        return classes;
    }

    private static List<String> classNames(Path projectRoot) {
        List<String> names = new ArrayList<>();
        for (String folder : SOURCE_FOLDERS) {
            Path sources = projectRoot.resolve(folder);
            if (!Files.isDirectory(sources)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(sources)) {
                files.filter(Files::isRegularFile)
                     .map(projectRoot::relativize)
                     .filter(ProjectClasses::isApplicationSource)
                     .sorted()
                     .map(ProjectClasses::className)
                     .forEach(names::add);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to list the Java sources of [" + sources + "]", ex);
            }
        }
        return names;
    }

    private static boolean isApplicationSource(Path file) {
        String fileName = file.getFileName()
                              .toString();
        return fileName.endsWith(JAVA_EXTENSION) && !NOT_CLASSES.contains(fileName) && !ClientTestSources.isTestSource(file);
    }

    /**
     * {@code gen/money/data/Line.java} declares {@code gen.money.data.Line} - the platform's layout.
     */
    private static String className(Path file) {
        List<String> segments = new ArrayList<>();
        file.forEach(segment -> segments.add(segment.toString()));
        String fileName = segments.removeLast();
        segments.add(fileName.substring(0, fileName.length() - JAVA_EXTENSION.length()));
        return String.join(".", segments);
    }
}
