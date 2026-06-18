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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.context.ApplicationEventPublisher;
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
 * <li>Write compiled {@code .class} files to the {@link JavaCompiledOutputDirectory} and delete
 * stale ones; publish a {@link JavaCompiledEvent} so listeners (e.g. JDT.LS) can react.</li>
 * </ol>
 * A class that fails to recompile (or whose batch produced no bytecode at all) keeps its last-good
 * bytecode in the new loader, so a single uncompilable source does not unload or delete the other
 * classes - only classes whose source was actually removed leave the generation. The prior
 * {@link ClassLoader} becomes unreachable once in-flight code releases its references.
 */
@Component
public class JavaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaLoader.class);

    private final JavaSourceCompiler compiler;
    private final ClientClassLoaderHolder loaderHolder;
    private final List<JavaClassConsumer> consumers;
    private final JavaCompiledOutputDirectory outputDirectory;
    private final ApplicationEventPublisher eventPublisher;

    /** FQN → {@link LoadedClass} record for the currently-installed generation. */
    private final Map<String, LoadedClass> currentGeneration = new HashMap<>();

    /**
     * Binary class name (top-level + nested) → bytecode for the currently-installed generation.
     * Retained so a class that fails to recompile this cycle can fall back to its last-good bytecode
     * instead of being unloaded and having its {@code .class} deleted.
     */
    private final Map<String, byte[]> currentBytecode = new HashMap<>();

    @Autowired
    public JavaLoader(JavaSourceCompiler compiler, ClientClassLoaderHolder loaderHolder, List<JavaClassConsumer> consumers,
            JavaCompiledOutputDirectory outputDirectory, ApplicationEventPublisher eventPublisher) {
        this.compiler = compiler;
        this.loaderHolder = loaderHolder;
        this.consumers = consumers;
        this.outputDirectory = outputDirectory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Recompile + reload the entire client code surface.
     *
     * @param sources every client {@code .java} that should be visible in the new generation (i.e.
     *        {@link org.eclipse.dirigible.engine.java.service.JavaFileService#getAll()} filtered for
     *        the ones whose source files currently exist in the registry, minus any that failed
     *        FQN-uniqueness pre-check)
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

        // Effective generation bytecode: carry over the LAST-GOOD bytecode for any class that still
        // has source this cycle but did not (re)compile, then overlay this cycle's fresh bytecode.
        // Classes whose source was deleted are dropped. This is what stops one uncompilable class from
        // clearing the others: javac can emit ZERO bytecode for the whole batch on some errors (e.g. an
        // unresolvable import aborts code generation for every unit), so survivors are preserved by
        // falling back to their previous bytecode instead of being unloaded and deleted.
        Set<String> submitted = new HashSet<>(fqnToProject.keySet());
        Set<String> freshlyCompiledOwners = new HashSet<>();
        for (String binaryName : batch.bytecode()
                                      .keySet()) {
            freshlyCompiledOwners.add(topLevelName(binaryName));
        }
        Map<String, byte[]> effectiveBytecode = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : currentBytecode.entrySet()) {
            String owner = topLevelName(entry.getKey());
            if (submitted.contains(owner) && !freshlyCompiledOwners.contains(owner)) {
                effectiveBytecode.put(entry.getKey(), entry.getValue());
            }
        }
        effectiveBytecode.putAll(batch.bytecode());

        // Build the new ClientClassLoader from the effective bytecode. Parent is the platform CL so
        // user code sees JavaHandler, our annotations, Spring, Hibernate, etc.
        ClassLoader parent = JavaHandler.class.getClassLoader();
        ClientClassLoader nextLoader = new ClientClassLoader(parent, effectiveBytecode);

        // Resolve every primary (top-level) FQN in the new generation through the new loader. The
        // generation includes freshly compiled classes AND last-good carry-overs; only the freshly
        // compiled ones are reported in succeededFqns.
        Map<String, LoadedClass> nextGeneration = new LinkedHashMap<>();
        Set<String> succeeded = new HashSet<>();
        Map<String, String> failures = new HashMap<>(batch.failures());
        for (String binaryName : effectiveBytecode.keySet()) {
            if (binaryName.indexOf('$') >= 0) {
                // A nested type's binary name (com.example.Outer$Inner): kept in the loader for linking
                // but not surfaced to consumers — only top-level classes flow through.
                continue;
            }
            String project = fqnToProject.get(binaryName);
            if (project == null) {
                continue; // not a submitted top-level source
            }
            try {
                Class<?> type = nextLoader.loadClass(binaryName);
                nextGeneration.put(binaryName, new LoadedClass(project, binaryName, type, nextLoader));
                if (freshlyCompiledOwners.contains(binaryName)) {
                    succeeded.add(binaryName);
                }
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.error("Compiled bytecode could not be loaded for [{}]: {}", binaryName, e.getMessage(), e);
                failures.put(binaryName, "Failed to load class [" + binaryName + "]: " + e.getMessage());
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
        currentBytecode.clear();
        currentBytecode.putAll(effectiveBytecode);

        RebuildResult result = new RebuildResult(Collections.unmodifiableSet(succeeded), Collections.unmodifiableMap(failures),
                Collections.unmodifiableSet(removed));

        // Writes this cycle's fresh bytecode and deletes only source-removed FQNs. Carried-over
        // (failed-to-recompile) classes keep their existing .class files untouched.
        writeClassFiles(batch, result.unloadedFqns());
        eventPublisher.publishEvent(new JavaCompiledEvent(this, result.succeededFqns(), result.unloadedFqns()));

        return result;
    }

    /**
     * Writes newly compiled {@code .class} files to the output directory and deletes files for FQNs
     * that were removed. Each file operation is guarded independently so a single failure does not
     * abort the rest.
     */
    private void writeClassFiles(JavaSourceCompiler.BatchResult batch, Set<String> unloadedFqns) {
        Path outDir = outputDirectory.get();

        for (Map.Entry<String, byte[]> entry : batch.bytecode()
                                                    .entrySet()) {
            String fqn = entry.getKey();
            // fqn may be a nested type (Outer$Inner) — convert dots to slashes but keep '$' intact.
            Path classFile = outDir.resolve(fqn.replace('.', '/') + ".class");
            try {
                Files.createDirectories(classFile.getParent());
                Files.write(classFile, entry.getValue());
            } catch (IOException e) {
                LOGGER.error("[java-lsp] Failed to write class file for [{}]: {}", fqn, e.getMessage(), e);
            }
        }

        for (String fqn : unloadedFqns) {
            Path classFile = outDir.resolve(fqn.replace('.', '/') + ".class");
            try {
                Files.deleteIfExists(classFile);
            } catch (IOException e) {
                LOGGER.warn("[java-lsp] Failed to delete stale class file for [{}]: {}", fqn, e.getMessage(), e);
            }
        }
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

    /** The top-level binary name for a class - strips a nested {@code $Inner} suffix. */
    private static String topLevelName(String binaryName) {
        int dollar = binaryName.indexOf('$');
        return dollar < 0 ? binaryName : binaryName.substring(0, dollar);
    }

    /** A client {@code .java} source as input to {@link #rebuild(List)}. */
    public record ClientSource(String project, String fqn, String source) {
    }

    /**
     * Per-FQN outcomes of a rebuild.
     *
     * @param succeededFqns FQNs that compiled successfully <em>this cycle</em> (not the whole live
     *        generation - classes kept on last-good bytecode are reported via {@code failures}, not
     *        here)
     * @param failures per-FQN compile error message for the ones that failed to (re)compile
     * @param unloadedFqns FQNs removed from the generation because their source was deleted
     */
    public record RebuildResult(Set<String> succeededFqns, Map<String, String> failures, Set<String> unloadedFqns) {
    }

}
