/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

/**
 * Which files the assistant ports to Java, and where the port goes (#8051).
 *
 * <p>
 * A TypeScript source and a JavaScript module run only on the server, so each is ported to a Java
 * class. A plain {@code .js} is not: it is either a page a browser loads or a static extension
 * module, which becomes a {@code .json} module rather than Java.
 */
final class ScriptPorts {

    private static final String TYPESCRIPT = ".ts";

    private static final String TYPESCRIPT_DECLARATION = ".d.ts";

    private static final String JAVASCRIPT_MODULE = ".mjs";

    private static final String JAVA = ".java";

    private ScriptPorts() {}

    /**
     * Tests whether a file is a server-side script the assistant ports to Java.
     *
     * @param path the project-relative path
     * @return true for a TypeScript source or a JavaScript module
     */
    static boolean isPortable(String path) {
        return (path.endsWith(TYPESCRIPT) && !path.endsWith(TYPESCRIPT_DECLARATION)) || path.endsWith(JAVASCRIPT_MODULE);
    }

    /**
     * The Java file a script is ported to by default: the same folder and name, which is the package
     * and the class name the client-Java runtime derives from it.
     *
     * @param path the script's project-relative path
     * @return the Java file's project-relative path
     */
    static String javaPathOf(String path) {
        return path.substring(0, path.lastIndexOf('.')) + JAVA;
    }

    /**
     * The script's language, as a Markdown fence names it.
     *
     * @param path the script's project-relative path
     * @return {@code typescript} or {@code javascript}
     */
    static String languageOf(String path) {
        return path.endsWith(TYPESCRIPT) ? "typescript" : "javascript";
    }
}
