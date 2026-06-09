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
 * Resolution order on {@link #findClass(String)}:
 * <ol>
 * <li>Defer to the parent (platform) classloader — every {@code dirigible-*} class and every
 * Flowable / Spring class is reachable here.</li>
 * <li>If the parent does not have it, consult the current {@link ClientClassLoader} (or fail with
 * {@link ClassNotFoundException} if no rebuild has happened yet).</li>
 * </ol>
 *
 * <p>
 * The holder is hot-swapped on every client rebuild, so {@link #findClass(String)} sees the latest
 * generation on first resolution. The JVM, however, records this loader as an <em>initiating
 * loader</em> for every name it has resolved through {@code findClass}; subsequent
 * {@code Class.forName(name, true, this)} calls from Flowable bypass {@code findClass} and return
 * the cached class from the previous generation. Hot-reload safety for the {@code class=} path
 * therefore requires a process restart. Users that need bulletproof hot-reload should use the
 * {@code ${JavaTask}} delegate-expression path (see {@code DirigibleJavaCallDelegate}) which
 * resolves through the holder every execution.
 */
final class ClientAwareClassLoader extends ClassLoader {

    private final ClientClassLoaderHolder holder;

    ClientAwareClassLoader(ClassLoader parent, ClientClassLoaderHolder holder) {
        super(parent);
        this.holder = holder;
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
