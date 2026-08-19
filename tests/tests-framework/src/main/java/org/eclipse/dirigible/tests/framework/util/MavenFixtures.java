/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds maven fixture artifacts for integration tests - file: repositories, plain library jars and
 * AOT compiled module jars (compiled classes + the {@code META-INF/dirigible/<project>/.compiled}
 * marker + a registry payload) - so the tests double as executable documentation of the module jar
 * format. Everything is produced locally with the JDK compiler; no test touches the network.
 */
public final class MavenFixtures {

    private MavenFixtures() {
        // utility
    }

    /**
     * Deploys a jar with a minimal POM into a file:-served fixture repository at the standard maven
     * layout.
     *
     * @param repositoryDir the fixture repository root
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param jar the jar to deploy
     */
    public static void deploy(Path repositoryDir, String groupId, String artifactId, String version, Path jar) {
        try {
            Path directory = Files.createDirectories(repositoryDir.resolve(groupId.replace('.', '/'))
                                                                  .resolve(artifactId)
                                                                  .resolve(version));
            Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"), """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>%s</version>
                    </project>
                    """.formatted(groupId, artifactId, version));
            Files.copy(jar, directory.resolve(artifactId + "-" + version + ".jar"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deploy [" + groupId + ":" + artifactId + ":" + version + "]", e);
        }
    }

    /**
     * Builds a plain library jar from the given sources - the "third-party dependency" fixture.
     *
     * @param workDir a scratch directory
     * @param jarName the jar file name
     * @param sources FQN to source
     * @return the built jar
     */
    public static Path buildPlainJar(Path workDir, String jarName, Map<String, String> sources) {
        return buildPlainJar(workDir, jarName, sources, Map.of());
    }

    /**
     * Builds a plain library jar from the given sources plus raw extra entries - e.g. a
     * {@code META-INF/services/java.sql.Driver} entry for a JDBC driver fixture.
     *
     * @param workDir a scratch directory
     * @param jarName the jar file name
     * @param sources FQN to source
     * @param extraEntries raw entries as entry name to content
     * @return the built jar
     */
    public static Path buildPlainJar(Path workDir, String jarName, Map<String, String> sources, Map<String, String> extraEntries) {
        Path classesDir = compile(workDir, jarName, sources);
        return jar(workDir.resolve(jarName), classesDir, null, extraEntries);
    }

    /**
     * Builds an AOT compiled module jar: the compiled classes, the
     * {@code META-INF/dirigible/<project>/.compiled} marker listing them, and the module's registry
     * payload under {@code META-INF/dirigible/<project>/}.
     *
     * @param workDir a scratch directory
     * @param jarName the jar file name
     * @param project the module's project name
     * @param sources FQN to source - each FQN is listed in the marker
     * @param payload registry payload as project-relative path to content
     * @return the built jar
     */
    public static Path buildModuleJar(Path workDir, String jarName, String project, Map<String, String> sources,
            Map<String, String> payload) {
        Path classesDir = compile(workDir, jarName, sources);
        String marker = sources.keySet()
                               .stream()
                               .sorted()
                               .collect(Collectors.joining("\n"));
        Map<String, String> metaInf = payload.entrySet()
                                             .stream()
                                             .collect(Collectors.toMap(entry -> "META-INF/dirigible/" + project + "/" + entry.getKey(),
                                                     Map.Entry::getValue));
        return jar(workDir.resolve(jarName), classesDir, "META-INF/dirigible/" + project + "/.compiled", metaInf, marker);
    }

    /**
     * Compiles the sources against the running test's classpath.
     *
     * @param workDir a scratch directory
     * @param jarName the jar name, used to keep scratch directories distinct
     * @param sources FQN to source
     * @return the classes directory
     */
    private static Path compile(Path workDir, String jarName, Map<String, String> sources) {
        try {
            Path sourcesDir = Files.createDirectories(workDir.resolve(jarName + "-sources"));
            Path classesDir = Files.createDirectories(workDir.resolve(jarName + "-classes"));
            String[] arguments = new String[sources.size() + 4];
            int index = 0;
            arguments[index++] = "-d";
            arguments[index++] = classesDir.toString();
            arguments[index++] = "-cp";
            arguments[index++] = System.getProperty("java.class.path");
            for (Map.Entry<String, String> source : sources.entrySet()) {
                Path file = sourcesDir.resolve(source.getKey()
                                                     .replace('.', '/')
                        + ".java");
                Files.createDirectories(file.getParent());
                Files.writeString(file, source.getValue());
                arguments[index++] = file.toString();
            }
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int exitCode = compiler.run(null, null, null, arguments);
            if (exitCode != 0) {
                throw new IllegalStateException("Fixture sources failed to compile: " + sources.keySet());
            }
            return classesDir;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compile the fixture sources", e);
        }
    }

    /**
     * Jar.
     *
     * @param jarPath the target jar path
     * @param classesDir the compiled classes
     * @param markerEntry the marker entry name, null for a plain jar
     * @param extraEntries extra entries as entry name to content
     * @param markerContent the marker content (used when markerEntry is set)
     * @return the jar path
     */
    private static Path jar(Path jarPath, Path classesDir, String markerEntry, Map<String, String> extraEntries, String... markerContent) {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath)); Stream<Path> files = Files.walk(classesDir)) {
            for (Path file : files.filter(Files::isRegularFile)
                                  .toList()) {
                out.putNextEntry(new JarEntry(classesDir.relativize(file)
                                                        .toString()
                                                        .replace('\\', '/')));
                out.write(Files.readAllBytes(file));
                out.closeEntry();
            }
            if (markerEntry != null) {
                out.putNextEntry(new JarEntry(markerEntry));
                out.write(markerContent[0].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            for (Map.Entry<String, String> entry : extraEntries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue()
                               .getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            return jarPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build the fixture jar [" + jarPath + "]", e);
        }
    }

}
