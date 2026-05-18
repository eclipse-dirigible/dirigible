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

import java.util.Map;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Glue between {@link JavaSourceCompiler} and {@link JavaClassRegistry}: compiles a single source
 * unit, defines its classes in a fresh {@link BytecodeClassLoader}, validates the primary type
 * implements {@link JavaHandler}, and registers the result.
 *
 * <p>
 * Each call produces a brand new {@link BytecodeClassLoader}, so a re-registration cleanly
 * supersedes the prior class identity — vital for hot reload.
 */
@Component
public class JavaLoader {

    private final JavaSourceCompiler compiler;
    private final JavaClassRegistry registry;

    @Autowired
    public JavaLoader(JavaSourceCompiler compiler, JavaClassRegistry registry) {
        this.compiler = compiler;
        this.registry = registry;
    }

    /**
     * Compile, load, and register a Java source.
     *
     * @param project the owning project (registry first-path-segment)
     * @param fqn fully-qualified class name parsed from the source
     * @param source raw Java source code
     * @return the freshly registered handler
     * @throws JavaCompilationException on compile error or if the primary class does not implement
     *         {@link JavaHandler}
     */
    public LoadedHandler loadAndRegister(String project, String fqn, String source) {
        Map<String, byte[]> bytecode = compiler.compile(fqn, source);

        // Parent is the loader that loaded the JavaHandler interface: that way user code can
        // refer to the platform-provided SPI and any other API on the application classpath.
        ClassLoader parent = JavaHandler.class.getClassLoader();
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, bytecode);

        Class<?> primary;
        try {
            primary = loader.loadClass(fqn);
        } catch (ClassNotFoundException e) {
            throw new JavaCompilationException("Compiled bytecode did not contain expected class [" + fqn + "]", e);
        }

        if (!JavaHandler.class.isAssignableFrom(primary)) {
            throw new JavaCompilationException(
                    "Class [" + fqn + "] must implement " + JavaHandler.class.getName() + " to be exposed as a Java endpoint");
        }

        @SuppressWarnings("unchecked")
        Class<? extends JavaHandler> handlerClass = (Class<? extends JavaHandler>) primary;

        LoadedHandler loaded = new LoadedHandler(project, fqn, loader, handlerClass);
        registry.register(loaded);
        return loaded;
    }

    /** Drop a handler from the registry. Returns {@code true} if something was removed. */
    public boolean unload(String project, String classFqn) {
        return registry.unregister(project, classFqn);
    }

}
