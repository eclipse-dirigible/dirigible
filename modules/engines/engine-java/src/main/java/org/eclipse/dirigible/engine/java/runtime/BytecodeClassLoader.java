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

/**
 * Defines classes from raw bytecode held in memory, delegating any reference outside the supplied
 * map to the platform classloader (parent).
 *
 * <p>
 * One instance per "deployment unit" (currently one source file's compilation output). Discarding
 * the instance and letting the bytecode buffers be garbage-collected is the entirety of the
 * unload path — there is no per-class lifecycle hook to invoke. Any thread/static state owned by
 * the user class must be cleaned up explicitly by the user code if it matters.
 *
 * <p>
 * Why a custom loader instead of {@code URLClassLoader} over a temp directory: keeping bytecode in
 * heap avoids creating loader leaks rooted in {@code URLClassLoader}'s file handle cache, removes
 * a class of "stale bytes on disk" failures, and makes the hot-reload path observable from a
 * single heap dump.
 */
final class BytecodeClassLoader extends ClassLoader {

    private final Map<String, byte[]> bytecode;

    BytecodeClassLoader(ClassLoader parent, Map<String, byte[]> bytecode) {
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

    /** Names of every class this loader can define directly (i.e. the unit's bytecode). */
    Map<String, byte[]> definedBytecode() {
        return Collections.unmodifiableMap(bytecode);
    }

}
