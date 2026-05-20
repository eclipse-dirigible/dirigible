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

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

/**
 * Wraps a Java source provided as a {@link String} so {@link javax.tools.JavaCompiler} can read it
 * without round-tripping through the filesystem.
 */
final class StringJavaSource extends SimpleJavaFileObject {

    private final String code;

    StringJavaSource(String binaryName, String code) {
        super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }

}
