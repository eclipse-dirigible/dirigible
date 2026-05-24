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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Batch compile + load + dispatch entry point for client {@code .java} sources.
 *
 * <p>
 * On each rebuild:
 * <ol>
 * <li>Hand all sources to {@link JavaSourceCompiler#compileBatch(java.util.List)} — a single
 * {@code javac} task so cross-file references in client code resolve.</li>
 * <li>Build a fresh {@link ClientClassLoader} from the resulting bytecode; install it under
 * {@link ClientClassLoaderHolder}.</li>
 * <li>Load every primary class from the new loader.</li>
 * <li>Compute the delta against the previous generation and notify every registered
 * {@link JavaClassConsumer}: {@code onClassUnloaded} for FQNs that disappeared or are being
 * replaced, then {@code onClassLoaded} for FQNs in the new generation.</li>
 * </ol>
 * No state from the previous generation survives in the active loader — the prior
 * {@link ClassLoader} becomes unreachable as soon as in-flight code releases its references.
 */
@Component
public class JavaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaLoader.class);

    private final JavaSourceCompiler compiler;
    private final ClientClassLoaderHolder loaderHolder;
    private final List<JavaClassConsumer> consumers;

    /** FQN → {@link LoadedClass} record for the currently-installed generation. */
    private final Map<String, LoadedClass> currentGeneration = new HashMap<>();

    @Autowired
    public JavaLoader(JavaSourceCompiler compiler, ClientClassLoaderHolder loaderHolder, List<JavaClassConsumer> consumers) {
        this.compiler = compiler;
        this.loaderHolder = loaderHolder;
        this.consumers = consumers;
    }

    /**
     * Recompile + reload the entire client code surface.
     *
     * @param sources every client {@code .java} that should be visible in the new generation (i.e.
     *        {@link JavaFileService#getAll()} filtered for the ones whose source files currently exist
     *        in the registry, minus any that failed FQN-uniqueness pre-check)
     * @return per-FQN outcomes — successes (in the new generation) and per-FQN compile error messages
     *         for the ones that failed to produce bytecode
     */
    public synchronized RebuildResult rebuild(List<ClientSource> sources) {
        List<JavaSourceCompiler.SourceUnit> compileUnits = new ArrayList<>(sources.size());
        Map<String, String> fqnToProject = new HashMap<>();
        for (ClientSource s : sources) {
            compileUnits.add(new JavaSourceCompiler.SourceUnit(s.fqn(), s.source()));
            fqnToProject.put(s.fqn(), s.project());
        }

        JavaSourceCompiler.BatchResult batch = compiler.compileBatch(compileUnits);

        // Build the new ClientClassLoader from successful bytecode. Parent is the platform CL so
        // user code sees JavaHandler, our annotations, Spring, Hibernate, etc.
        ClassLoader parent = JavaHandler.class.getClassLoader();
        ClientClassLoader nextLoader = new ClientClassLoader(parent, batch.bytecode());

        // Resolve every successfully-compiled primary FQN through the new loader.
        Map<String, LoadedClass> nextGeneration = new LinkedHashMap<>();
        Map<String, String> failures = new HashMap<>(batch.failures());
        for (String fqn : batch.bytecode()
                               .keySet()) {
            String project = fqnToProject.get(fqn);
            if (project == null) {
                // A nested type's binary name (com.example.Outer$Inner) wasn't listed as a top-level
                // source. We keep its bytecode in the loader (so reflective access via outer class
                // works) but we don't notify consumers for it — only top-level classes flow through.
                continue;
            }
            try {
                Class<?> type = nextLoader.loadClass(fqn);
                nextGeneration.put(fqn, new LoadedClass(project, fqn, type, nextLoader));
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.error("Compiled bytecode could not be loaded for [{}]: {}", fqn, e.getMessage(), e);
                failures.put(fqn, "Failed to load class [" + fqn + "]: " + e.getMessage());
            }
        }

        // Diff against the previous generation: notify consumers of removals first, then additions.
        Set<String> previousFqns = new HashSet<>(currentGeneration.keySet());
        Set<String> nextFqns = new HashSet<>(nextGeneration.keySet());

        Set<String> removed = new HashSet<>(previousFqns);
        removed.removeAll(nextFqns);
        List<LoadedClass> toUnload = new ArrayList<>();
        for (String fqn : removed) {
            toUnload.add(currentGeneration.get(fqn));
        }
        Set<String> replaced = new HashSet<>(previousFqns);
        replaced.retainAll(nextFqns);
        for (String fqn : replaced) {
            toUnload.add(currentGeneration.get(fqn));
        }
        // Consumer-outer / class-inner: every consumer drains its claimed classes before the next
        // consumer runs. Combined with Spring's @Order on the consumers, this lets dependents
        // (e.g. ControllerClassConsumer satisfying @Inject) see their providers (e.g.
        // RepositoryClassConsumer) already registered within the same rebuild cycle.
        notifyAll(consumers, toUnload, /* loaded */ false);

        // Install the loader BEFORE notifying onClassLoaded so consumers see consistent state via
        // the holder if they look it up.
        loaderHolder.swap(nextLoader);

        notifyAll(consumers, new ArrayList<>(nextGeneration.values()), /* loaded */ true);

        currentGeneration.clear();
        currentGeneration.putAll(nextGeneration);

        return new RebuildResult(Collections.unmodifiableSet(nextGeneration.keySet()), Collections.unmodifiableMap(failures),
                Collections.unmodifiableSet(removed));
    }

    /**
     * Iterate each consumer in {@code @Order} sequence and drain its claimed classes before moving on.
     * {@code loaded=true} dispatches {@code onClassLoaded}, {@code false} dispatches
     * {@code onClassUnloaded}.
     */
    private static void notifyAll(List<JavaClassConsumer> consumers, List<LoadedClass> classes, boolean loaded) {
        for (JavaClassConsumer consumer : consumers) {
            for (LoadedClass info : classes) {
                if (info == null) {
                    continue;
                }
                try {
                    if (consumer.accepts(info.type())) {
                        if (loaded) {
                            consumer.onClassLoaded(info);
                        } else {
                            consumer.onClassUnloaded(info);
                        }
                    }
                } catch (Exception | LinkageError e) {
                    // LinkageError (NoClassDefFoundError / ClassFormatError / …) is recoverable in a
                    // hot-reload context — a controller whose @Inject field-type failed to compile must
                    // not abort the whole rebuild and unregister every other working controller.
                    // VirtualMachineError / ThreadDeath still propagate.
                    LOGGER.error("Consumer [{}] threw on {} for [{}]: {}", consumer.getClass()
                                                                                   .getSimpleName(),
                            loaded ? "onClassLoaded" : "onClassUnloaded", info.fqn(), e.getMessage(), e);
                }
            }
        }
    }

    /** A client {@code .java} source as input to {@link #rebuild(List)}. */
    public record ClientSource(String project, String fqn, String source) {
    }

    /** Per-FQN outcomes of a rebuild. */
    public record RebuildResult(Set<String> succeededFqns, Map<String, String> failures, Set<String> unloadedFqns) {
    }

}
