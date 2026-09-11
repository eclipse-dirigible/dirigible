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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * In-process compilation of Java sources via the JDK Java Compiler API.
 *
 * <p>
 * Sources are passed as strings; emitted bytecode is captured in memory by
 * {@link InMemoryJavaFileManager}. The compile-time classpath is supplied by {@link ClassPathIndex}
 * as a list of on-disk paths and bound to the standard file manager via
 * {@link StandardJavaFileManager#setLocationFromPaths setLocationFromPaths(CLASS_PATH, ...)}.
 *
 * <p>
 * The {@link #compileBatch(List)} entry point exists because Dirigible's single shared
 * {@code ClientClassLoader} compiles every client source in a single {@code javac} task so
 * cross-file references inside user code resolve naturally.
 */
@Component
public class JavaSourceCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceCompiler.class);

    /**
     * Upper bound on the salvage rounds of {@link #compileBatch(List)}. Each round removes at least one
     * unit, so the fixpoint is reached in as many rounds as the dependency chain is deep - two or three
     * in practice. The bound only stops a pathological batch from compiling once per unit.
     */
    private static final int MAX_COMPILE_ROUNDS = 5;

    private final ClassPathIndex classPathIndex;

    /** Test-only convenience: uses an empty classpath index (relies on {@code java.class.path}). */
    public JavaSourceCompiler() {
        this(new ClassPathIndex(new ModulesClassLoaderHolder()));
    }

    @Autowired
    public JavaSourceCompiler(ClassPathIndex classPathIndex) {
        this.classPathIndex = classPathIndex;
    }


    /** A Java source as input to {@link #compileBatch(List)}. */
    public record SourceUnit(String fqn, String source) {
    }


    /**
     * Result of a batch compilation.
     *
     * @param bytecode compiled binary class name → bytecode (top-level + nested types)
     * @param failures per top-level FQN → a single formatted failure message (kept for existing
     *        call-sites that surface one error string per artefact)
     * @param diagnostics per top-level FQN → the structured diagnostics behind that failure, so
     *        consumers can render one entry per error at its line/column; absent for FQNs that failed
     *        without an attributable diagnostic (e.g. produced no class file)
     */
    public record BatchResult(Map<String, byte[]> bytecode, Map<String, String> failures,
            Map<String, List<CompileDiagnostic>> diagnostics) {
    }

    /**
     * Compile a single Java source. Convenience wrapper over {@link #compileBatch(List)} that throws on
     * any failure to keep its existing call-sites stable.
     *
     * @param fqn fully-qualified binary class name (e.g. {@code com.example.HelloHandler})
     * @param source the source text
     * @return map of binary class name → bytecode (top-level + nested types from this unit)
     * @throws JavaCompilationException if {@code javac} is unavailable or compilation fails
     */
    public Map<String, byte[]> compile(String fqn, String source) {
        BatchResult result = compileBatch(List.of(new SourceUnit(fqn, source)));
        if (!result.failures.isEmpty()) {
            String message = result.failures.values()
                                            .iterator()
                                            .next();
            throw new JavaCompilationException(message);
        }
        if (result.bytecode.isEmpty()) {
            throw new JavaCompilationException("Compilation of [" + fqn + "] produced no class files");
        }
        return result.bytecode;
    }

    /**
     * Compile multiple sources together so cross-file references resolve, keeping the output of the
     * units that do compile.
     *
     * <p>
     * {@code javac} stops before code generation as soon as the batch holds an error, so a single
     * unresolvable import emits <b>no bytecode at all</b> - including for units with no error of their
     * own and no relation to the broken one. This method therefore compiles in rounds: a round that
     * leaves such collateral units drops the ones {@code javac} attributed an error to and compiles the
     * remainder again, until nothing more can be salvaged. A unit that genuinely depends on a dropped
     * one fails the next round with its own {@code cannot find symbol} diagnostic and is dropped in
     * turn, so the outcome is the maximal compilable subset plus a per-unit reason for every unit left
     * out of it.
     *
     * <p>
     * A batch that compiles cleanly runs exactly one {@code javac} task, as before; the extra rounds
     * are paid only on the error path and are bounded by {@link #MAX_COMPILE_ROUNDS}.
     */
    public BatchResult compileBatch(List<SourceUnit> units) {
        if (units.isEmpty()) {
            return new BatchResult(Map.of(), Map.of(), Map.of());
        }

        List<SourceUnit> remaining = new ArrayList<>(units);
        Map<String, String> failures = new LinkedHashMap<>();
        Map<String, List<CompileDiagnostic>> diagnostics = new LinkedHashMap<>();
        Map<String, byte[]> bytecode;
        int round = 1;

        while (true) {
            Attempt attempt = compileOnce(remaining);
            bytecode = attempt.bytecode();
            Set<String> rejected = attempt.rejected();

            // Collateral damage: a unit reported as failed although javac had nothing to say about it -
            // it simply never reached code generation. Dropping the units javac DID reject and
            // compiling the rest is what recovers their output.
            boolean salvageable = attempt.failures()
                                         .size() > rejected.size()
                    && !rejected.isEmpty() && rejected.size() < remaining.size();

            if (!salvageable) {
                collect(failures, diagnostics, attempt, attempt.failures()
                                                               .keySet());
                break;
            }
            if (round >= MAX_COMPILE_ROUNDS) {
                LOGGER.warn("Giving up on salvaging the batch after [{}] compile rounds - [{}] unit(s) still without output",
                        MAX_COMPILE_ROUNDS, attempt.failures()
                                                   .size());
                collect(failures, diagnostics, attempt, attempt.failures()
                                                               .keySet());
                break;
            }

            collect(failures, diagnostics, attempt, rejected);
            remaining.removeIf(unit -> rejected.contains(unit.fqn()));
            round++;
        }

        if (failures.isEmpty()) {
            // INFO, not DEBUG: the batch runs only when the source set changed, and after a
            // mid-publish failure this line is the ONLY visible evidence that the next cycle
            // recovered - at DEBUG the stale failure ERROR stays the log's last word and a
            // green system diagnoses as dead.
            LOGGER.info("Compiled batch: [{}] units, [{}] class file(s), no failures", units.size(), bytecode.size());
        } else if (round == 1) {
            LOGGER.error("Compiled batch: [{}] units, [{}] class file(s); [{}] unit(s) failed to compile: {}", units.size(),
                    bytecode.size(), failures.size(), failures.keySet());
        } else {
            LOGGER.error("Compiled batch: [{}] units, [{}] class file(s) over [{}] compile rounds; [{}] unit(s) failed to compile: {}",
                    units.size(), bytecode.size(), round, failures.size(), failures.keySet());
        }

        return new BatchResult(bytecode, Collections.unmodifiableMap(failures), Collections.unmodifiableMap(diagnostics));
    }

    /**
     * Carry the given units' failure message and structured diagnostics over from one round's attempt
     * into the batch-wide outcome. A unit is recorded by the round that rejected it, so its message is
     * the one javac produced while the unit was still in the batch.
     */
    private static void collect(Map<String, String> failures, Map<String, List<CompileDiagnostic>> diagnostics, Attempt attempt,
            Set<String> fqns) {
        for (String fqn : fqns) {
            String message = attempt.failures()
                                    .get(fqn);
            if (message != null) {
                failures.put(fqn, message);
            }
            List<CompileDiagnostic> unitDiagnostics = attempt.diagnostics()
                                                             .get(fqn);
            if (unitDiagnostics != null) {
                diagnostics.put(fqn, unitDiagnostics);
            }
        }
    }

    /**
     * The outcome of one {@code javac} task.
     *
     * @param bytecode compiled binary class name to bytecode
     * @param failures per top-level FQN, a single formatted failure message
     * @param diagnostics per top-level FQN, the structured diagnostics behind that failure
     * @param rejected the FQNs {@code javac} attributed at least one error to - as opposed to the ones
     *        that merely produced no class file because the task stopped before code generation
     */
    private record Attempt(Map<String, byte[]> bytecode, Map<String, String> failures, Map<String, List<CompileDiagnostic>> diagnostics,
            Set<String> rejected) {
    }

    /** Runs a single {@code javac} task over the given units. */
    private Attempt compileOnce(List<SourceUnit> units) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new JavaCompilationException("System Java compiler is not available. " + "Ensure the runtime is a JDK, not a JRE.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
                InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standard)) {

            List<Path> classpath = new ArrayList<>(classPathIndex.classPathEntries());
            if (!classpath.isEmpty()) {
                standard.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            }

            List<JavaFileObject> compilationUnits = new ArrayList<>(units.size());
            for (SourceUnit u : units) {
                compilationUnits.add(new StringJavaSource(u.fqn(), u.source()));
            }
            List<String> options = Arrays.asList("-proc:none", "-g");

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
            task.call();

            Map<String, byte[]> bytecode = fileManager.compiledClasses();

            // Log every diagnostic javac emitted — including warnings and any error that can't be
            // attributed to a specific source unit (classpath / option errors carry no source) — so
            // no compilation error is lost between here and the per-FQN bucketing below. A rejected
            // unit is dropped from the next round, so no diagnostic is logged twice.
            logDiagnostics(diagnostics);

            Buckets buckets = bucketDiagnostics(units, bytecode, diagnostics);
            return new Attempt(bytecode, buckets.failures(), buckets.diagnostics(), buckets.rejected());

        } catch (Exception e) {
            throw new JavaCompilationException("Batch compilation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Log every diagnostic the compiler produced, one line each. Errors are logged at {@code ERROR},
     * warnings at {@code WARN}, notes/other at {@code DEBUG}. This guarantees that diagnostics which
     * {@link #bucketDiagnostics} can't tie back to a source unit (e.g. a missing classpath entry, which
     * javac reports with no {@code source}) still surface in the log.
     */
    private static void logDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            JavaFileObject source = d.getSource();
            String where = source != null ? source.getName() + ":" + d.getLineNumber() + ":" + d.getColumnNumber() : "<no source>";
            String message = d.getMessage(Locale.ROOT);
            switch (d.getKind()) {
                case ERROR -> LOGGER.error("javac error at {}: {}", where, message);
                case WARNING, MANDATORY_WARNING -> LOGGER.warn("javac warning at {}: {}", where, message);
                default -> LOGGER.debug("javac {} at {}: {}", d.getKind(), where, message);
            }
        }
    }

    /**
     * Build a {@code Map<FQN, message>} of compilation failures keyed by the source's top-level FQN. A
     * unit is considered failed if it produced no class file <em>or</em> if any error-level diagnostic
     * in the batch references its source file (by simple-name match — javac doesn't give us back the
     * original {@code JavaFileObject} we passed in for every diagnostic). The second group is reported
     * separately as {@code rejected}: those are the units javac actually refused, and dropping exactly
     * them is what lets the next round compile the units that only lost their output.
     */
    private record Buckets(Map<String, String> failures, Map<String, List<CompileDiagnostic>> diagnostics, Set<String> rejected) {
    }

    private static Buckets bucketDiagnostics(List<SourceUnit> units, Map<String, byte[]> bytecode,
            DiagnosticCollector<JavaFileObject> diagnostics) {

        Map<String, List<Diagnostic<? extends JavaFileObject>>> perUnit = new LinkedHashMap<>();
        Set<String> unitFqns = new HashSet<>();
        for (SourceUnit u : units) {
            unitFqns.add(u.fqn());
            perUnit.put(u.fqn(), new ArrayList<>());
        }
        List<Diagnostic<? extends JavaFileObject>> orphan = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            JavaFileObject src = d.getSource();
            String matched = null;
            if (src != null) {
                String uri = src.toUri()
                                .toString();
                for (String fqn : unitFqns) {
                    String tail = "/" + fqn.replace('.', '/') + ".java";
                    if (uri.endsWith(tail)) {
                        matched = fqn;
                        break;
                    }
                }
            }
            if (matched != null) {
                perUnit.get(matched)
                       .add(d);
            } else {
                orphan.add(d);
            }
        }

        Map<String, String> failures = new HashMap<>();
        Map<String, List<CompileDiagnostic>> structured = new HashMap<>();
        Set<String> rejected = new HashSet<>();
        for (SourceUnit u : units) {
            String fqn = u.fqn();
            boolean hasBytecode = bytecode.containsKey(fqn);
            List<Diagnostic<? extends JavaFileObject>> errs = perUnit.get(fqn);
            if (!errs.isEmpty()) {
                failures.put(fqn, formatDiagnostics(fqn, errs));
                structured.put(fqn, toCompileDiagnostics(errs));
                rejected.add(fqn);
            } else if (!hasBytecode) {
                // No class file and no error we could attribute to this unit: surface the orphan
                // diagnostics (e.g. a classpath error with no source) when there are any, else a
                // generic message. Orphan diagnostics have no usable position, so attribute them to
                // every no-bytecode unit as the best available explanation.
                if (orphan.isEmpty()) {
                    failures.put(fqn, "Compilation produced no class file for [" + fqn + "]");
                } else {
                    failures.put(fqn, formatDiagnostics(fqn, orphan));
                    structured.put(fqn, toCompileDiagnostics(orphan));
                }
            }
        }
        return new Buckets(Collections.unmodifiableMap(failures), Collections.unmodifiableMap(structured),
                Collections.unmodifiableSet(rejected));
    }

    private static List<CompileDiagnostic> toCompileDiagnostics(List<Diagnostic<? extends JavaFileObject>> diags) {
        List<CompileDiagnostic> result = new ArrayList<>(diags.size());
        for (Diagnostic<? extends JavaFileObject> d : diags) {
            result.add(new CompileDiagnostic(d.getKind() == Diagnostic.Kind.ERROR, d.getLineNumber(), d.getColumnNumber(),
                    d.getMessage(Locale.ROOT)));
        }
        return result;
    }

    private static String formatDiagnostics(String fqn, List<Diagnostic<? extends JavaFileObject>> errs) {
        StringBuilder sb = new StringBuilder("Compilation of [").append(fqn)
                                                                .append("] failed:");
        for (Diagnostic<? extends JavaFileObject> d : errs) {
            sb.append(System.lineSeparator())
              .append("  ")
              .append(d.getKind())
              .append(" line ")
              .append(d.getLineNumber())
              .append(": ")
              .append(d.getMessage(Locale.ROOT));
        }
        return sb.toString();
    }

}
