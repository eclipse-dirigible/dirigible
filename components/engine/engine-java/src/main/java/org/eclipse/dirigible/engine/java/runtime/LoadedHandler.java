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

import org.eclipse.dirigible.engine.java.handler.JavaHandler;

/**
 * A successfully compiled + loaded user handler, together with the dedicated {@link ClassLoader}
 * that defined its {@link Class}.
 *
 * <p>
 * The {@link ClassLoader} is retained so the {@link JavaClassRegistry} can drop the reference on
 * update/delete and let GC reclaim the class metadata (Metaspace) — the central correctness
 * invariant for hot reload.
 */
public final class LoadedHandler {

    private final String project;
    private final String classFqn;
    private final ClassLoader loader;
    private final Class<? extends JavaHandler> handlerClass;

    public LoadedHandler(String project, String classFqn, ClassLoader loader, Class<? extends JavaHandler> handlerClass) {
        this.project = project;
        this.classFqn = classFqn;
        this.loader = loader;
        this.handlerClass = handlerClass;
    }

    public String getProject() {
        return project;
    }

    public String getClassFqn() {
        return classFqn;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public Class<? extends JavaHandler> getHandlerClass() {
        return handlerClass;
    }

    /** Instantiate a fresh handler. We do not pool instances; user code must be stateless. */
    public JavaHandler newInstance() throws ReflectiveOperationException {
        return handlerClass.getDeclaredConstructor()
                           .newInstance();
    }

}
