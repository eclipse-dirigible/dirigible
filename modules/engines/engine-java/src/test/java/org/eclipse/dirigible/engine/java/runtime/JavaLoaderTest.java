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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JavaLoaderTest {

    private static final String PROJECT = "sample-project";
    private static final String FQN = "client.SampleHandler";

    private JavaLoader loader;
    private JavaClassRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new JavaClassRegistry();
        loader = new JavaLoader(new JavaSourceCompiler(), registry);
    }

    @Test
    void compiles_loads_and_registers_handler() throws Exception {
        loader.loadAndRegister(PROJECT, FQN, """
                package client;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                public class SampleHandler implements JavaHandler {
                    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.getWriter().write("first");
                    }
                }
                """);
        LoadedHandler loaded = registry.find(PROJECT, FQN)
                                       .orElseThrow();
        JavaHandler instance = loaded.newInstance();
        // Smoke test: instance is the right shape; full HTTP behaviour tested at endpoint level.
        assertTrue(instance instanceof JavaHandler);
    }

    @Test
    void reload_replaces_class_loader_so_old_class_identity_is_dropped() {
        loader.loadAndRegister(PROJECT, FQN, sampleSource("first"));
        LoadedHandler before = registry.find(PROJECT, FQN)
                                       .orElseThrow();

        loader.loadAndRegister(PROJECT, FQN, sampleSource("second"));
        LoadedHandler after = registry.find(PROJECT, FQN)
                                      .orElseThrow();

        // Different ClassLoader instance — proves the prior bytecode buffer + class identity is
        // no longer referenced from the registry path.
        assertNotSame(before.getLoader(), after.getLoader());
        assertNotSame(before.getHandlerClass(), after.getHandlerClass());
    }

    @Test
    void rejects_class_that_does_not_implement_handler() {
        JavaCompilationException ex = assertThrows(JavaCompilationException.class, () -> loader.loadAndRegister(PROJECT, "client.Plain",
                """
                        package client;
                        public class Plain {}
                        """));
        assertTrue(ex.getMessage()
                     .contains("JavaHandler"));
    }

    @Test
    void unload_removes_registry_entry() {
        loader.loadAndRegister(PROJECT, FQN, sampleSource("x"));
        assertEquals(1, registry.size());
        assertTrue(loader.unload(PROJECT, FQN));
        assertEquals(0, registry.size());
        assertFalse(loader.unload(PROJECT, FQN));
    }

    private static String sampleSource(String body) {
        return """
                package client;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                public class SampleHandler implements JavaHandler {
                    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.getWriter().write("%s");
                    }
                }
                """.formatted(body);
    }

    // Static smoke check at import-resolution time
    @SuppressWarnings("unused")
    private static final Class<?> SERVLET_PRESENT = HttpServletRequest.class;
    @SuppressWarnings("unused")
    private static final Class<?> SERVLET_RESPONSE_PRESENT = HttpServletResponse.class;

}
