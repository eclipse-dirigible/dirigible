/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.eclipse.dirigible.engine.java.runtime.ClientClassLoader;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;

/**
 * ClassLoader handed to Flowable so {@code flowable:class="my.fqn.MyClass"} on a service task can
 * resolve to a class compiled from a {@code .java} file in the user's project.
 *
 * <p>
 * {@link #loadClass(String)} always delegates to the <em>current</em> {@link ClientClassLoader}
 * (whose own parent is the platform classloader, so every {@code dirigible-*} / Flowable / Spring
 * class stays reachable). This is deliberately <em>not</em> the standard
 * {@code findLoadedClass → parent → findClass} delegation: this loader instance is captured once by
 * Flowable's {@code CommandContextInterceptor} at engine boot and cannot be swapped at runtime, so
 * if it cached resolutions itself (or was used as the initiating loader for
 * {@code Class.forName(name, true, this)}) it would keep returning the class from the first
 * generation and a recompiled delegate would need a server restart. Routing every call to
 * {@code holder.current().loadClass(name)} makes the fixed instance a transparent pass-through to
 * the latest generation instead.
 *
 * <p>
 * Two collaborators make this effective: {@code BpmFlowableConfig} sets
 * {@code useClassForNameClassLoading = false} so Flowable resolves delegates via
 * {@code ClassLoader.loadClass} (this override) rather than {@code Class.forName} (which the JVM
 * caches against the loader instance); and {@code FlowableClientClassLoaderRefresher} evicts the
 * parsed-process cache on every rebuild, because Flowable caches the instantiated delegate on the
 * parsed service task. The {@code ${JavaTask}} delegate-expression path (see
 * {@code DirigibleJavaCallDelegate}) resolves through the holder on every execution and is
 * hot-reload-safe by construction.
 */
final class ClientAwareClassLoader extends ClassLoader {

    private final ClientClassLoaderHolder holder;

    ClientAwareClassLoader(ClassLoader parent, ClientClassLoaderHolder holder) {
        super(parent);
        this.holder = holder;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ClientClassLoader current = holder.current();
        if (current == null) {
            // No client code compiled yet — fall back to the platform delegation.
            return super.loadClass(name);
        }
        return current.loadClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ClientClassLoader current = holder.current();
        if (current == null) {
            throw new ClassNotFoundException(name);
        }
        return current.loadClass(name);
    }

}
