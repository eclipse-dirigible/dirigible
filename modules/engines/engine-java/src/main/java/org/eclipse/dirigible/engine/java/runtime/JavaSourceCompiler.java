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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * In-process compilation of a single Java source via the JDK Java Compiler API.
 *
 * <p>
 * Sources are read from a {@link String} and emitted bytecode is captured in memory by
 * {@link InMemoryJavaFileManager}. The compile-time classpath is supplied by
 * {@link ClassPathIndex} as a list of on-disk paths and bound to the standard file manager via
 * {@link StandardJavaFileManager#setLocationFromPaths setLocationFromPaths(CLASS_PATH, ...)}.
 */
@Component
public class JavaSourceCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceCompiler.class);

    private final ClassPathIndex classPathIndex;

    @Autowired
    public JavaSourceCompiler(ClassPathIndex classPathIndex) {
        this.classPathIndex = classPathIndex;
    }

    /** Test-only convenience: uses an empty classpath index (relies on {@code java.class.path}). */
    public JavaSourceCompiler() {
        this(new ClassPathIndex());
    }

    /**
     * Compile a Java source.
     *
     * @param fqn fully-qualified binary class name (e.g. {@code com.example.HelloHandler})
     * @param source the source text
     * @return map of binary class name → bytecode (top-level + nested types from this unit)
     * @throws JavaCompilationException if {@code javac} is unavailable or compilation fails
     */
    public Map<String, byte[]> compile(String fqn, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new JavaCompilationException("System Java compiler is not available. "
                    + "Ensure the runtime is a JDK, not a JRE.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
                InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standard)) {

            List<Path> classpath = new ArrayList<>(classPathIndex.classPathEntries());
            if (!classpath.isEmpty()) {
                standard.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            }

            List<JavaFileObject> compilationUnits = List.of(new StringJavaSource(fqn, source));
            // -proc:none — user code should not run annotation processors at runtime.
            // -g — preserve line numbers for readable stack traces on failure.
            List<String> options = Arrays.asList("-proc:none", "-g");

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();
            if (!success) {
                String message = formatDiagnostics(fqn, diagnostics);
                throw new JavaCompilationException(message);
            }

            Map<String, byte[]> classes = fileManager.compiledClasses();
            if (classes.isEmpty()) {
                throw new JavaCompilationException("Compilation of [" + fqn + "] produced no class files");
            }
            LOGGER.debug("Compiled [{}] -> {} class file(s)", fqn, classes.size());
            return classes;

        } catch (JavaCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new JavaCompilationException("Compilation of [" + fqn + "] failed: " + e.getMessage(), e);
        }
    }

    private static String formatDiagnostics(String fqn, DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder("Compilation of [").append(fqn)
                                                                 .append("] failed:");
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
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
