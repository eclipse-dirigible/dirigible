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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.handler.HandlerClassConsumer;
import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JavaLoaderTest {

    private static final String PROJECT = "sample-project";
    private static final String FQN = "client.SampleHandler";

    /**
     * Shared no-op publisher. Implements both {@code publishEvent} overloads explicitly so call sites
     * with a static {@code ApplicationEvent} type resolve unambiguously — the lambda form
     * {@code (Object o) -> {}} is flagged by CodeQL's {@code java/confusing-method-signature} because
     * the {@code publishEvent(ApplicationEvent)} default would otherwise be the more specific match.
     */
    private static final ApplicationEventPublisher NOOP_PUBLISHER = new ApplicationEventPublisher() {
        @Override
        public void publishEvent(ApplicationEvent event) {
            // intentionally empty
        }

        @Override
        public void publishEvent(Object event) {
            // intentionally empty
        }
    };

    @TempDir
    Path tempDir;

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
        JavaCompiledOutputDirectory outputDirectory = mock(JavaCompiledOutputDirectory.class);
        when(outputDirectory.get()).thenReturn(tempDir);
        loader = new JavaLoader(new JavaSourceCompiler(), holder, new ComponentContainer(new ClientBeansHolder()),
                List.of(handlerConsumer, recording), outputDirectory, NOOP_PUBLISHER);
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
    void consumer_throwing_linkage_error_does_not_abort_rebuild_for_other_classes() {
        // Reproduces the bug: ControllerClassConsumer.injectDependencies() walks getDeclaredFields()
        // on a controller whose @Inject field type failed to compile, the JVM throws
        // NoClassDefFoundError, and *without* this fix the whole rebuild aborts — every other
        // already-rebuilt controller stays unregistered. With the fix in place, the throwing consumer
        // is logged and skipped, and the recording consumer still observes the second class.
        ThrowingConsumer throwingConsumer = new ThrowingConsumer("client.Bomb");
        ClientClassLoaderHolder freshHolder = new ClientClassLoaderHolder();
        JavaCompiledOutputDirectory outputDirectory = mock(JavaCompiledOutputDirectory.class);
        when(outputDirectory.get()).thenReturn(tempDir);
        JavaLoader localLoader = new JavaLoader(new JavaSourceCompiler(), freshHolder, new ComponentContainer(new ClientBeansHolder()),
                List.of(throwingConsumer, recording), outputDirectory, NOOP_PUBLISHER);

        localLoader.rebuild(List.of(handlerSource("client.Bomb", "boom"), handlerSource("client.Bystander", "fine")));

        // Both classes compiled — proven by the recording consumer observing them.
        assertEquals(2, recording.loaded.size(), "recording consumer must still see every class after a sibling throws LinkageError");
        assertTrue(recording.loaded.stream()
                                   .anyMatch(c -> c.fqn()
                                                   .equals("client.Bystander")));
    }

    @Test
    void compile_error_is_isolated_to_the_offending_unit() {
        JavaLoader.RebuildResult result =
                loader.rebuild(List.of(handlerSource("client.Good", "ok"), new JavaLoader.ClientSource(PROJECT, "client.Broken", """
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

    /**
     * Multi-project, two-cycle proof of the question "if I break one class, do the others survive?".
     *
     * <p>
     * Five projects, two {@code JavaHandler} classes each (10 total) compile cleanly on the first cycle
     * and register. On the second cycle exactly one class ({@code p2.C0}) is given an unresolvable
     * import ({@code import asdasdasd.DoesNotExist;}) - the worst case, because that error makes
     * {@code javac} emit ZERO bytecode for the <em>entire</em> batch. The assertions prove the
     * keep-last-good contract:
     * <ul>
     * <li>the broken class is reported in {@code failures} but is NOT reported as succeeded;</li>
     * <li>nothing is unloaded ({@code unloadedFqns} is empty) - the generation is not cleared;</li>
     * <li>all ten handlers stay registered on their last-good version (including the broken one, so its
     * dependents keep linking);</li>
     * <li>all ten {@code .class} files remain on disk - a single bad import does not wipe the
     * others.</li>
     * </ul>
     * The numbers (5x2) are arbitrary; the behaviour is identical at 5x10.
     */
    @Test
    void breaking_one_class_keeps_every_other_class_on_disk_and_in_the_generation() {
        List<JavaLoader.ClientSource> good = new ArrayList<>();
        for (int p = 0; p < 5; p++) {
            for (int c = 0; c < 2; c++) {
                good.add(projectHandler("p" + p, "C" + c, "v1"));
            }
        }

        JavaLoader.RebuildResult first = loader.rebuild(good);
        assertEquals(10, first.succeededFqns()
                              .size(),
                "all ten classes compile on the first cycle");
        assertTrue(first.failures()
                        .isEmpty(),
                "no failures on the first cycle");
        assertEquals(10, handlerRegistry.size(), "all ten handlers register on the first cycle");
        for (JavaLoader.ClientSource s : good) {
            assertTrue(Files.exists(classFile(s.fqn())), "missing .class after first build: " + s.fqn());
        }

        // Second cycle: break exactly one class (p2.C0) with an unresolvable import. This is the worst
        // case - javac emits ZERO bytecode for the whole batch - so it is the strongest test of "do the
        // others survive".
        String brokenFqn = "p2.C0";
        List<JavaLoader.ClientSource> next = new ArrayList<>();
        for (JavaLoader.ClientSource s : good) {
            if (s.fqn()
                 .equals(brokenFqn)) {
                next.add(new JavaLoader.ClientSource("p2", brokenFqn, """
                        package p2;
                        import asdasdasd.DoesNotExist;
                        public class C0 {
                            DoesNotExist broken;
                        }
                        """));
            } else {
                next.add(s);
            }
        }

        JavaLoader.RebuildResult second = loader.rebuild(next);

        // The broken class is reported failed, but is NOT counted as succeeded this cycle.
        assertTrue(second.failures()
                         .containsKey(brokenFqn),
                "the broken class is reported as a failure");
        assertFalse(second.succeededFqns()
                          .contains(brokenFqn),
                "the broken class is not reported as succeeded");

        // Nothing is unloaded and nothing is deleted: every class keeps its last-good version, in the
        // live generation AND on disk.
        assertTrue(second.unloadedFqns()
                         .isEmpty(),
                "no class is unloaded when a sibling fails to recompile");
        assertEquals(10, handlerRegistry.size(), "all ten handlers stay registered on their last-good version");
        assertTrue(handlerRegistry.find("p2", brokenFqn)
                                  .isPresent(),
                "the broken class keeps running its last-good version (so its dependents still link)");
        for (JavaLoader.ClientSource s : good) {
            assertTrue(Files.exists(classFile(s.fqn())),
                    ".class cleared for " + s.fqn() + " - a single bad import must not wipe the others");
        }
    }

    /**
     * The isolated-error regime: when {@code javac} can still emit bytecode for the good sibling (a
     * method-body error rather than a whole-batch abort), the good sibling recompiles to its new
     * version while the broken one falls back to its last-good version - it is neither unloaded nor
     * deleted.
     */
    @Test
    void recompile_failure_keeps_last_good_while_the_good_sibling_recompiles() {
        loader.rebuild(List.of(projectHandler("px", "A", "v1"), projectHandler("px", "B", "v1")));
        assertEquals(2, handlerRegistry.size());

        JavaLoader.RebuildResult second =
                loader.rebuild(List.of(projectHandler("px", "A", "v2"), new JavaLoader.ClientSource("px", "px.B", """
                        package px;
                        import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                        import jakarta.servlet.http.HttpServletRequest;
                        import jakarta.servlet.http.HttpServletResponse;
                        public class B implements JavaHandler {
                            public void handle(HttpServletRequest req, HttpServletResponse resp) {
                                thisMethodDoesNotExist();
                            }
                        }
                        """)));

        assertTrue(second.succeededFqns()
                         .contains("px.A"),
                "the good sibling recompiles to its new version");
        assertTrue(second.failures()
                         .containsKey("px.B"),
                "the broken class is reported as failed");
        assertTrue(second.unloadedFqns()
                         .isEmpty(),
                "the broken class is not unloaded - it keeps last-good");
        assertEquals(2, handlerRegistry.size(), "both classes remain registered");
        assertTrue(handlerRegistry.find("px", "px.B")
                                  .isPresent(),
                "B keeps running its last-good version");
        assertTrue(Files.exists(classFile("px.A")), "A's recompiled .class is present");
        assertTrue(Files.exists(classFile("px.B")), "B's last-good .class is retained");
    }

    /** A compilable {@code JavaHandler} for {@code <project>.<simple>}, package = project name. */
    private static JavaLoader.ClientSource projectHandler(String project, String simple, String body) {
        return new JavaLoader.ClientSource(project, project + "." + simple, """
                package %s;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                public class %s implements JavaHandler {
                    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        resp.getWriter().write("%s");
                    }
                }
                """.formatted(project, simple, body));
    }

    /** On-disk location of a compiled class within the loader's output directory. */
    private Path classFile(String fqn) {
        return tempDir.resolve(fqn.replace('.', '/') + ".class");
    }

    private static JavaLoader.ClientSource handlerSource(String fqn, String body) {
        int dot = fqn.lastIndexOf('.');
        String pkg = dot >= 0 ? fqn.substring(0, dot) : "";
        String simple = dot >= 0 ? fqn.substring(dot + 1) : fqn;
        return new JavaLoader.ClientSource(PROJECT, fqn, """
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

    /**
     * Test consumer that throws {@link NoClassDefFoundError} on {@code onClassLoaded} for one named
     * FQN, accepts every class. Mirrors the production failure mode where
     * {@code ControllerClassConsumer.injectDependencies}'s {@code getDeclaredFields()} fails because a
     * field type wasn't compiled.
     */
    private static final class ThrowingConsumer implements JavaClassConsumer {
        private final String poisonFqn;

        ThrowingConsumer(String poisonFqn) {
            this.poisonFqn = poisonFqn;
        }

        @Override
        public boolean accepts(Class<?> clazz) {
            return true;
        }

        @Override
        public void onClassLoaded(LoadedClass info) {
            if (poisonFqn.equals(info.fqn())) {
                throw new NoClassDefFoundError("simulated missing dep on " + info.fqn());
            }
        }

        @Override
        public void onClassUnloaded(LoadedClass info) {}
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
