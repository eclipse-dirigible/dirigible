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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * Captures bytecode emitted by {@link javax.tools.JavaCompiler} into in-memory buffers keyed by
 * binary class name.
 *
 * <p>
 * Classpath enumeration is delegated entirely to the wrapped {@link StandardJavaFileManager},
 * which is configured by {@link JavaSourceCompiler} with explicit on-disk paths via
 * {@link StandardJavaFileManager#setLocationFromPaths}. See {@link ClassPathIndex} for the
 * rationale (Spring Boot 3 nested-jar handles must not be touched in-process).
 */
final class InMemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private final Map<String, ByteArrayJavaClass> classes = new HashMap<>();

    InMemoryJavaFileManager(StandardJavaFileManager wrapped) {
        super(wrapped);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind,
            FileObject sibling) throws IOException {
        if (kind != JavaFileObject.Kind.CLASS) {
            throw new IOException("Unsupported output kind for in-memory compilation: " + kind);
        }
        ByteArrayJavaClass file = new ByteArrayJavaClass(className);
        classes.put(className, file);
        return file;
    }

    Map<String, byte[]> compiledClasses() {
        Map<String, byte[]> out = new HashMap<>(classes.size());
        for (Map.Entry<String, ByteArrayJavaClass> e : classes.entrySet()) {
            out.put(e.getKey(), e.getValue()
                                 .getBytes());
        }
        return Collections.unmodifiableMap(out);
    }

    /** A {@link JavaFileObject} whose output is captured into a {@link ByteArrayOutputStream}. */
    private static final class ByteArrayJavaClass extends SimpleJavaFileObject {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ByteArrayJavaClass(String binaryName) {
            super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return buffer;
        }

        byte[] getBytes() {
            return buffer.toByteArray();
        }
    }

}
