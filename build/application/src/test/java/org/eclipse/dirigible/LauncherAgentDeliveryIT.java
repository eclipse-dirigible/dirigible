/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the launcher-agent delivery of the production launch configuration: the executable jar
 * carries the {@code Launcher-Agent-Class} manifest entry, launches through
 * {@code PropertiesLauncher} (the ZIP layout), holds the agent classes at its root with the nested
 * BOOT-INF jars still STORED, and an actual {@code java -jar} launch prints the agent's boot marker
 * before the application starts - proof the JVM handed the agent its {@code Instrumentation}.
 */
class LauncherAgentDeliveryIT {

    /** The agent's boot marker (see DirigibleLauncherAgent#install). */
    private static final String AGENT_BOOT_MARKER = "Dirigible launcher agent installed";

    @TempDir
    Path tempDir;

    @Test
    void the_executable_jar_carries_the_agent_delivery() throws IOException {
        Path executableJar = executableJar();
        try (JarFile jar = new JarFile(executableJar.toFile())) {
            Manifest manifest = jar.getManifest();
            assertEquals("org.eclipse.dirigible.launcher.agent.DirigibleLauncherAgent", manifest.getMainAttributes()
                                                                                                .getValue("Launcher-Agent-Class"),
                    "the JVM reads the launcher agent from this manifest entry on -jar launches");
            assertEquals("org.springframework.boot.loader.launch.PropertiesLauncher", manifest.getMainAttributes()
                                                                                              .getValue("Main-Class"),
                    "the ZIP layout keeps loader.path working under -jar");

            assertNotNull(jar.getEntry("org/eclipse/dirigible/launcher/agent/DirigibleLauncherAgent.class"),
                    "the agent class must sit at the jar ROOT - only the root is on the system classpath");
            assertNotNull(jar.getEntry("org/eclipse/dirigible/launcher/agent/InstrumentationHolder.class"),
                    "the holder class must sit at the jar ROOT next to the agent");

            // the root-class injection must not have recompressed the nested jars - Spring Boot's
            // loader requires them STORED
            JarEntry nestedJar = jar.stream()
                                    .filter(entry -> entry.getName()
                                                          .startsWith("BOOT-INF/lib/")
                                            && entry.getName()
                                                    .endsWith(".jar"))
                                    .findFirst()
                                    .orElseThrow();
            assertEquals(ZipEntry.STORED, nestedJar.getMethod(), "nested BOOT-INF jars must stay STORED after the agent injection");
        }
    }

    @Test
    void the_executable_jar_carries_the_provided_bom() throws IOException {
        try (JarFile jar = new JarFile(executableJar().toFile())) {
            ZipEntry bom = jar.getEntry("BOOT-INF/classes/META-INF/dirigible-provided-bom.xml");
            assertNotNull(bom, "the provided-BOM must ship inside the artifact - the resolver's shadowing detection reads it");
            String content = new String(jar.getInputStream(bom)
                                           .readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(content.contains("<artifactId>dirigible-provided-bom</artifactId>"),
                    "the embedded BOM must be the standard dependencyManagement POM");
            assertTrue(content.contains("<groupId>com.google.code.gson</groupId>"),
                    "the BOM must enumerate the platform's BOOT-INF/lib inventory");
        }
    }

    @Test
    void a_jar_launch_installs_the_agent_before_main() throws IOException, InterruptedException {
        Path workingDirectory = Files.createDirectories(tempDir.resolve("launch"));
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", executableJar().toAbsolutePath()
                                                                                   .toString());
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(true);
        // an unclaimed port, so the probe never clashes with a locally running instance; the
        // process is destroyed long before it would bind anything
        builder.environment()
               .put("DIRIGIBLE_SERVER_PORT", "0");

        Process process = builder.start();
        boolean markerSeen = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(90);
            String line;
            while (System.currentTimeMillis() < deadline && (line = reader.readLine()) != null) {
                if (line.contains(AGENT_BOOT_MARKER)) {
                    markerSeen = true;
                    break;
                }
            }
        } finally {
            process.destroyForcibly();
            process.waitFor(30, TimeUnit.SECONDS);
        }
        assertTrue(markerSeen, "the java -jar launch must print the agent's boot marker before the application starts");
    }

    /**
     * The executable jar this module just packaged - the newest one, so a stale jar of a previous
     * version surviving in a non-clean target directory is never picked.
     *
     * @return the jar path
     */
    private static Path executableJar() {
        try (Stream<Path> files = Files.list(Path.of("target"))) {
            return files.filter(file -> file.getFileName()
                                            .toString()
                                            .endsWith("-executable.jar"))
                        .max(java.util.Comparator.comparingLong(file -> file.toFile()
                                                                            .lastModified()))
                        .orElseThrow(() -> new IllegalStateException("the executable jar is not in target - run the package phase first"));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list the target directory", e);
        }
    }

}
