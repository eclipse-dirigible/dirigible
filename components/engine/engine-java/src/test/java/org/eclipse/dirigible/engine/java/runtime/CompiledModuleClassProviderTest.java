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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.ToolProvider;

import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies {@link CompiledModuleClassProvider} discovers a
 * {@code META-INF/dirigible/<project>/.compiled} marker through the given classloader, derives the
 * project from the path, and loads the listed class through that classloader - no runtime
 * compilation.
 */
class CompiledModuleClassProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void discovers_marker_and_loads_the_listed_class() {
        // javaLoader is unused by discover(); the classpath carries the test fixture marker at
        // src/test/resources/META-INF/dirigible/aot-test-mod/.compiled.
        CompiledModuleClassProvider provider = new CompiledModuleClassProvider(null, null);

        List<LoadedClass> discovered = provider.discover(getClass().getClassLoader());

        LoadedClass fixture = discovered.stream()
                                        .filter(c -> c.fqn()
                                                      .equals(AotFixtureClass.class.getName()))
                                        .findFirst()
                                        .orElse(null);
        assertNotNull(fixture, "the class listed in the .compiled marker must be discovered");
        assertEquals("aot-test-mod", fixture.project(), "project is the path segment under META-INF/dirigible");
        assertSame(AotFixtureClass.class, fixture.type(), "the class is loaded via the application classloader (no compile)");
    }

    @Test
    void discovers_a_marker_through_a_custom_classloader() throws IOException, ClassNotFoundException {
        Path moduleJar = aotModuleJar("dyn-mod", "dynmod.DynamicFixture");
        CompiledModuleClassProvider provider = new CompiledModuleClassProvider(null, null);

        try (ModulesClassLoader modules = new ModulesClassLoader(getClass().getClassLoader(), List.of(moduleJar))) {
            List<LoadedClass> discovered = provider.discover(modules);

            LoadedClass fixture = discovered.stream()
                                            .filter(c -> "dynmod.DynamicFixture".equals(c.fqn()))
                                            .findFirst()
                                            .orElse(null);
            assertNotNull(fixture, "the marker inside a modules-classloader jar must be discovered");
            assertEquals("dyn-mod", fixture.project());
            assertSame(modules, fixture.type()
                                       .getClassLoader(),
                    "the class is loaded through the given classloader (no compile)");
        }
    }

    /** Builds an AOT module jar: one compiled class + its {@code .compiled} marker. */
    private Path aotModuleJar(String project, String fqn) throws IOException {
        String packageName = fqn.substring(0, fqn.lastIndexOf('.'));
        String className = fqn.substring(fqn.lastIndexOf('.') + 1);
        Path sourceDir = Files.createDirectories(tempDir.resolve("src")
                                                        .resolve(packageName));
        Path source = sourceDir.resolve(className + ".java");
        Files.writeString(source, "package %s; public class %s {}".formatted(packageName, className));
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        int exitCode = ToolProvider.getSystemJavaCompiler()
                                   .run(null, null, null, "-d", classesDir.toString(), source.toString());
        assertEquals(0, exitCode);

        Path jar = tempDir.resolve(project + ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            String classEntry = fqn.replace('.', '/') + ".class";
            out.putNextEntry(new JarEntry(classEntry));
            out.write(Files.readAllBytes(classesDir.resolve(classEntry)));
            out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/dirigible/" + project + "/.compiled"));
            out.write(fqn.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

}
