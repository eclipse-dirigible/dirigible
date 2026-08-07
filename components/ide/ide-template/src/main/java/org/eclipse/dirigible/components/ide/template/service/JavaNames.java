/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service;

import javax.lang.model.SourceVersion;

/**
 * Derives valid Java names from the free-text file and project names a user enters when scaffolding
 * from a template.
 *
 * <p>
 * Templates that generate client Java need both: the class name, because javac requires a public
 * class to live in a file of the same name, and the package name, because the client compiler keys
 * classes by fully-qualified name across all projects — two projects scaffolding the same handler
 * in the default package would collide.
 */
final class JavaNames {

    /** Used when the entered file name carries no usable identifier characters at all. */
    private static final String CLASS_NAME_FALLBACK = "Handler";

    /** Used when the project name carries no usable identifier characters at all. */
    private static final String PACKAGE_NAME_FALLBACK = "app";

    private JavaNames() {}

    /**
     * Converts an entered file name into a valid PascalCase Java class name.
     *
     * <p>
     * Every run of characters that cannot appear in a Java identifier acts as a word separator, and
     * each remaining word is capitalized: {@code my-job} and {@code my job} both become {@code MyJob}.
     * A leading digit is prefixed with an underscore so the result is a legal identifier. Since the
     * result always starts with an upper-case letter or an underscore, it can never collide with a Java
     * keyword.
     *
     * @param fileNameBase the entered file name without its extension, may be null
     * @return a valid Java class name, never null or blank
     */
    static String toClassName(String fileNameBase) {
        if (fileNameBase == null) {
            return CLASS_NAME_FALLBACK;
        }
        StringBuilder className = new StringBuilder(fileNameBase.length());
        boolean startOfWord = true;
        for (int index = 0; index < fileNameBase.length(); index++) {
            char character = fileNameBase.charAt(index);
            if (!Character.isJavaIdentifierPart(character)) {
                startOfWord = true;
                continue;
            }
            className.append(startOfWord ? Character.toUpperCase(character) : character);
            startOfWord = false;
        }
        if (className.isEmpty()) {
            return CLASS_NAME_FALLBACK;
        }
        if (!Character.isJavaIdentifierStart(className.charAt(0))) {
            className.insert(0, '_');
        }
        return className.toString();
    }

    /**
     * Converts a project name into a valid single-segment, lower-case Java package name.
     *
     * <p>
     * Characters that cannot appear in a Java identifier are dropped, so {@code my-app} and
     * {@code My App} both become {@code myapp}. A leading digit is prefixed with an underscore, and a
     * name that would otherwise be a keyword or a reserved literal ({@code package}, {@code true}) is
     * suffixed with one.
     *
     * @param projectName the name of the project being scaffolded into, may be null
     * @return a valid package name, never null or blank
     */
    static String toPackageName(String projectName) {
        if (projectName == null) {
            return PACKAGE_NAME_FALLBACK;
        }
        StringBuilder packageName = new StringBuilder(projectName.length());
        for (int index = 0; index < projectName.length(); index++) {
            char character = projectName.charAt(index);
            if (Character.isJavaIdentifierPart(character)) {
                packageName.append(Character.toLowerCase(character));
            }
        }
        if (packageName.isEmpty()) {
            return PACKAGE_NAME_FALLBACK;
        }
        if (!Character.isJavaIdentifierStart(packageName.charAt(0))) {
            packageName.insert(0, '_');
        }
        if (SourceVersion.isKeyword(packageName)) {
            packageName.append('_');
        }
        return packageName.toString();
    }

}
