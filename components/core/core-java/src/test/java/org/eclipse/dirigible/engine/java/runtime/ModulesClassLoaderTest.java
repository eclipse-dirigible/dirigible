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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The modules layer contract: parent-first delegation, visibility from child
 * {@link ClientClassLoader} generations, and independence of two generations over different JAR
 * sets.
 */
class ModulesClassLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void delegates_parent_first() throws Exception {
        Path parentJar = jarWithClass("parent-lib", "com.example.Dup", "parent");
        Path childJar = jarWithClass("child-lib", "com.example.Dup", "child");

        try (URLClassLoader parent = new URLClassLoader(new java.net.URL[] {parentJar.toUri()
                                                                                     .toURL()},
                getClass().getClassLoader()); ModulesClassLoader modules = new ModulesClassLoader(parent, List.of(childJar))) {

            Class<?> resolved = modules.loadClass("com.example.Dup");

            assertThat(value(resolved)).isEqualTo("parent");
            assertThat(resolved.getClassLoader()).isSameAs(parent);
        }
    }

    @Test
    void loads_a_module_only_class_and_is_visible_from_a_client_generation() throws Exception {
        Path moduleJar = jarWithClass("module-lib", "com.example.ModuleOnly", "from-module");

        try (ModulesClassLoader modules = new ModulesClassLoader(getClass().getClassLoader(), List.of(moduleJar))) {
            ClientClassLoader clientGeneration = new ClientClassLoader(modules, Map.of());

            Class<?> resolved = clientGeneration.loadClass("com.example.ModuleOnly");

            assertThat(value(resolved)).isEqualTo("from-module");
            assertThat(resolved.getClassLoader()).isSameAs(modules);
            assertThat(modules.getName()).isEqualTo(ModulesClassLoader.NAME);
        }
    }

    @Test
    void two_generations_over_different_jar_sets_are_independent() throws Exception {
        Path jarA = jarWithClass("lib-a", "com.example.OnlyA", "a");
        Path jarB = jarWithClass("lib-b", "com.example.OnlyB", "b");

        try (ModulesClassLoader generationOne = new ModulesClassLoader(getClass().getClassLoader(), List.of(jarA));
                ModulesClassLoader generationTwo = new ModulesClassLoader(getClass().getClassLoader(), List.of(jarB))) {

            assertThat(value(generationOne.loadClass("com.example.OnlyA"))).isEqualTo("a");
            assertThat(value(generationTwo.loadClass("com.example.OnlyB"))).isEqualTo("b");
            assertThatThrownBy(() -> generationTwo.loadClass("com.example.OnlyA")).isInstanceOf(ClassNotFoundException.class);
            assertThat(generationOne.jars()).containsExactly(jarA);
            assertThat(generationTwo.jars()).containsExactly(jarB);
        }
    }

    @Test
    void holder_serves_an_empty_generation_until_the_first_swap_and_counts_generations() throws Exception {
        ModulesClassLoaderHolder holder = new ModulesClassLoaderHolder();

        assertThat(holder.generation()).isZero();
        assertThat(holder.current()
                         .jars()).isEmpty();
        // the pre-swap generation behaves as plain delegation to the application classloader
        assertThat(holder.current()
                         .loadClass(ModulesClassLoaderTest.class.getName())).isSameAs(ModulesClassLoaderTest.class);

        Path jar = jarWithClass("swap-lib", "com.example.Swapped", "v1");
        ModulesClassLoader swapped = holder.swap(List.of(jar));

        assertThat(holder.generation()).isEqualTo(1);
        assertThat(holder.current()).isSameAs(swapped);
        assertThat(value(holder.current()
                               .loadClass("com.example.Swapped"))).isEqualTo("v1");
    }

    private static String value(Class<?> type) throws Exception {
        return (String) type.getMethod("value")
                            .invoke(type.getDeclaredConstructor()
                                        .newInstance());
    }

    /**
     * Compiles a single class whose {@code value()} answers the given literal and packs it in a jar.
     */
    private Path jarWithClass(String jarName, String fqn, String value) throws IOException {
        int lastDot = fqn.lastIndexOf('.');
        String packageName = fqn.substring(0, lastDot);
        String className = fqn.substring(lastDot + 1);
        Path sourceDir = Files.createDirectories(tempDir.resolve(jarName + "-src")
                                                        .resolve(packageName.replace('.', '/')));
        Path source = sourceDir.resolve(className + ".java");
        Files.writeString(source, """
                package %s;
                public class %s {
                    public String value() {
                        return "%s";
                    }
                }
                """.formatted(packageName, className, value));
        Path classesDir = Files.createDirectories(tempDir.resolve(jarName + "-classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int exitCode = compiler.run(null, null, null, "-d", classesDir.toString(), source.toString());
        assertThat(exitCode).isZero();

        Path jar = tempDir.resolve(jarName + ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar)); Stream<Path> files = Files.walk(classesDir)) {
            for (Path file : files.filter(Files::isRegularFile)
                                  .toList()) {
                out.putNextEntry(new JarEntry(classesDir.relativize(file)
                                                        .toString()
                                                        .replace('\\', '/')));
                out.write(Files.readAllBytes(file));
                out.closeEntry();
            }
        }
        return jar;
    }

}
