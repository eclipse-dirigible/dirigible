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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.engine.java.handler.HandlerClassConsumer;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JavaLoaderTest {

    private static final String PROJECT = "sample-project";
    private static final String FQN = "client.SampleHandler";

    private JavaClassRegistry handlerRegistry;
    private ClientClassLoaderHolder holder;
    private RecordingConsumer recording;
    private HandlerClassConsumer handlerConsumer;
    private JavaLoader loader;

    @BeforeEach
    void setUp() {
        handlerRegistry = new JavaClassRegistry();
        holder = new ClientClassLoaderHolder();
        handlerConsumer = new HandlerClassConsumer(handlerRegistry);
        recording = new RecordingConsumer();
        loader = new JavaLoader(new JavaSourceCompiler(), holder, List.of(handlerConsumer, recording));
    }

    @Test
    void handler_class_is_registered_and_consumed() {
        JavaLoader.RebuildResult result = loader.rebuild(List.of(handlerSource(FQN, "first")));

        assertTrue(result.succeededFqns()
                         .contains(FQN));
        assertTrue(result.failures()
                         .isEmpty());

        LoadedHandler loaded = handlerRegistry.find(PROJECT, FQN)
                                              .orElseThrow();
        assertNotNull(loaded);
        assertSame(holder.current(), loaded.getLoader(), "registered handler shares the active client classloader");

        // Recording consumer accepts every class — proves the SPI fan-out works.
        assertEquals(1, recording.loaded.size());
        assertEquals(FQN, recording.loaded.get(0)
                                          .fqn());
    }

    @Test
    void rebuild_replaces_class_loader_and_notifies_consumers_of_the_swap() {
        loader.rebuild(List.of(handlerSource(FQN, "v1")));
        ClassLoader firstLoader = holder.current();
        Class<?> firstClass = handlerRegistry.find(PROJECT, FQN)
                                             .orElseThrow()
                                             .getHandlerClass();

        loader.rebuild(List.of(handlerSource(FQN, "v2")));
        ClassLoader secondLoader = holder.current();
        Class<?> secondClass = handlerRegistry.find(PROJECT, FQN)
                                              .orElseThrow()
                                              .getHandlerClass();

        assertNotSame(firstLoader, secondLoader);
        assertNotSame(firstClass, secondClass);

        // Two onClassLoaded (one per rebuild) and one onClassUnloaded (for the replacement).
        assertEquals(2, recording.loaded.size());
        assertEquals(1, recording.unloaded.size());
        assertEquals(FQN, recording.unloaded.get(0)
                                            .fqn());
    }

    @Test
    void removing_source_in_next_rebuild_fires_unload() {
        loader.rebuild(List.of(handlerSource(FQN, "v1")));
        assertEquals(1, recording.loaded.size());
        assertEquals(0, recording.unloaded.size());

        loader.rebuild(List.of()); // empty rebuild = nothing left
        assertEquals(1, recording.unloaded.size());
        assertEquals(0, handlerRegistry.size(), "handler registry empties after consumer onClassUnloaded");
    }

    @Test
    void non_handler_class_is_loaded_but_not_in_handler_registry() {
        JavaLoader.RebuildResult result = loader.rebuild(List.of(new JavaLoader.ClientSource(PROJECT, "client.Plain", """
                package client;
                public class Plain {
                    public String greet() { return "hi"; }
                }
                """)));

        assertTrue(result.succeededFqns()
                         .contains("client.Plain"));
        assertTrue(result.failures()
                         .isEmpty());
        assertEquals(0, handlerRegistry.size(), "Plain isn't a JavaHandler so the handler registry stays empty");
        assertEquals(1, recording.loaded.size(), "recording consumer (accepts everything) still sees it");
    }

    @Test
    void compile_error_is_isolated_to_the_offending_unit() {
        JavaLoader.RebuildResult result = loader.rebuild(List.of(handlerSource("client.Good", "ok"), new JavaLoader.ClientSource(PROJECT,
                "client.Broken", """
                        package client;
                        public class Broken {
                            void m() { thisMethodDoesNotExist(); }
                        }
                        """)));

        assertTrue(result.succeededFqns()
                         .contains("client.Good"));
        assertTrue(result.failures()
                         .containsKey("client.Broken"));
        assertNotNull(handlerRegistry.find(PROJECT, "client.Good")
                                     .orElse(null));
    }

    private static JavaLoader.ClientSource handlerSource(String fqn, String body) {
        int dot = fqn.lastIndexOf('.');
        String pkg = dot >= 0 ? fqn.substring(0, dot) : "";
        String simple = dot >= 0 ? fqn.substring(dot + 1) : fqn;
        return new JavaLoader.ClientSource(PROJECT, fqn,
                """
                        package %s;
                        import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                        import jakarta.servlet.http.HttpServletRequest;
                        import jakarta.servlet.http.HttpServletResponse;
                        public class %s implements JavaHandler {
                            public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                                resp.getWriter().write("%s");
                            }
                        }
                        """.formatted(pkg, simple, body));
    }

    /** Test consumer that records every load/unload — accepts every class. */
    private static final class RecordingConsumer implements JavaClassConsumer {
        final List<LoadedClass> loaded = new ArrayList<>();
        final List<LoadedClass> unloaded = new ArrayList<>();

        @Override
        public boolean accepts(Class<?> clazz) {
            return true;
        }

        @Override
        public void onClassLoaded(LoadedClass info) {
            loaded.add(info);
        }

        @Override
        public void onClassUnloaded(LoadedClass info) {
            unloaded.add(info);
        }
    }

    // Static smoke check at import-resolution time
    @SuppressWarnings("unused")
    private static final Class<?> SERVLET_PRESENT = HttpServletRequest.class;
    @SuppressWarnings("unused")
    private static final Class<?> SERVLET_RESPONSE_PRESENT = HttpServletResponse.class;
    @SuppressWarnings("unused")
    private static final Class<?> HANDLER_PRESENT = JavaHandler.class;

}
