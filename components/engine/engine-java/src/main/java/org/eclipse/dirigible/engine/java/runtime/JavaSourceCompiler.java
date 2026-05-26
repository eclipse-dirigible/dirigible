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

    private final ClassPathIndex classPathIndex;

    /** Test-only convenience: uses an empty classpath index (relies on {@code java.class.path}). */
    public JavaSourceCompiler() {
        this(new ClassPathIndex());
    }

    @Autowired
    public JavaSourceCompiler(ClassPathIndex classPathIndex) {
        this.classPathIndex = classPathIndex;
    }


    /** A Java source as input to {@link #compileBatch(List)}. */
    public record SourceUnit(String fqn, String source) {
    }


    /** Result of a batch compilation. */
    public record BatchResult(Map<String, byte[]> bytecode, Map<String, String> failures) {
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
     * Compile multiple sources together so cross-file references resolve. The compiler is invoked once;
     * per-source diagnostics are sorted into {@link BatchResult#failures()}, successful bytecode into
     * {@link BatchResult#bytecode()}.
     *
     * <p>
     * Note: {@code javac} may still produce class files for units that don't depend on a broken unit.
     * If a single unit fails its own type-check, only that unit is reported as failed; the others
     * remain in {@link BatchResult#bytecode()}.
     */
    public BatchResult compileBatch(List<SourceUnit> units) {
        if (units.isEmpty()) {
            return new BatchResult(Map.of(), Map.of());
        }
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
            // no compilation error is lost between here and the per-FQN bucketing below.
            logDiagnostics(diagnostics);

            Map<String, String> failures = bucketDiagnostics(units, bytecode, diagnostics);

            if (failures.isEmpty()) {
                LOGGER.debug("Compiled batch: [{}] units, [{}] class file(s), no failures", units.size(), bytecode.size());
            } else {
                LOGGER.error("Compiled batch: [{}] units, [{}] class file(s); [{}] unit(s) failed to compile: {}", units.size(),
                        bytecode.size(), failures.size(), failures.keySet());
            }

            return new BatchResult(bytecode, failures);

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
     * original {@code JavaFileObject} we passed in for every diagnostic).
     */
    private static Map<String, String> bucketDiagnostics(List<SourceUnit> units, Map<String, byte[]> bytecode,
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
        for (SourceUnit u : units) {
            String fqn = u.fqn();
            boolean hasBytecode = bytecode.containsKey(fqn);
            List<Diagnostic<? extends JavaFileObject>> errs = perUnit.get(fqn);
            if (!errs.isEmpty()) {
                failures.put(fqn, formatDiagnostics(fqn, errs));
            } else if (!hasBytecode) {
                String message = orphan.isEmpty() ? "Compilation produced no class file for [" + fqn + "]" : formatDiagnostics(fqn, orphan);
                failures.put(fqn, message);
            }
        }
        return Collections.unmodifiableMap(failures);
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
