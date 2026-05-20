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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines classes from raw bytecode held in memory. A <b>single</b> instance is alive at a time
 * (managed by {@link ClientClassLoaderHolder}) and holds every successfully-compiled client
 * {@code .java} across every project — so any client class can reference any other by FQN.
 *
 * <p>
 * Each rebuild produces a fresh instance; the prior loader becomes unreachable as soon as no
 * in-flight request still holds a Class loaded from it and the JVM reclaims its Metaspace at the
 * next GC.
 *
 * <p>
 * Why a custom loader instead of {@code URLClassLoader} over a temp directory: keeping bytecode in
 * heap avoids creating loader leaks rooted in {@code URLClassLoader}'s file handle cache, removes a
 * class of "stale bytes on disk" failures, and makes the hot-reload path observable from a single
 * heap dump.
 */
public final class ClientClassLoader extends ClassLoader {

    private final Map<String, byte[]> bytecode;

    public ClientClassLoader(ClassLoader parent, Map<String, byte[]> bytecode) {
        super(parent);
        this.bytecode = new HashMap<>(bytecode);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = bytecode.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    /** FQNs that this loader can define directly (i.e. the current generation's client classes). */
    public Set<String> definedFqns() {
        return Collections.unmodifiableSet(bytecode.keySet());
    }

}
