/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript.modules.java;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Class JavaPackageProxyGenerator.
 */
public class JavaPackageProxyGenerator {

    /**
     * Generate.
     *
     * @param javaPackageName the java package name
     * @return the string
     */
    public String generate(String javaPackageName) {
        List<ClassName> classesInPackage = getClassesInPackage(javaPackageName);
        return generateJavaExports(classesInPackage);
    }

    /**
     * Gets the classes in package.
     *
     * @param packageName the package name
     * @return the classes in package
     */
    private List<ClassName> getClassesInPackage(String packageName) {
        try (ScanResult scanResult = new ClassGraph().verbose()
                                                     .enableClassInfo()
                                                     .enableSystemJarsAndModules()
                                                     .acceptPackages(packageName)
                                                     .scan()) {

            return scanResult.getAllClasses()
                             .stream()
                             .filter(c -> c.isPublic() && !c.isSynthetic() && !c.isAnonymousInnerClass() && !c.isAnnotation()
                                     && !c.isInnerClass())
                             .map(c -> new ClassName(c.getSimpleName(), c.getName()))
                             .distinct()
                             .collect(Collectors.toList());
        }
    }

    /**
     * Generate java exports.
     *
     * @param classes the classes
     * @return the string
     */
    private String generateJavaExports(List<ClassName> classes) {
        StringBuilder exportsBuilder = new StringBuilder();
        List<String> exportedSymbolNames = new ArrayList<String>();

        for (ClassName klass : classes) {
            String className = klass.className;
            if (!isAlphanumeric(className)) {
                continue;
            }

            exportsBuilder.append("export const ")
                          .append(className)
                          .append(" = Java.type('")
                          .append(klass.packageAndClassName)
                          .append("');")
                          .append(System.lineSeparator());

            exportedSymbolNames.add(className);
        }

        exportsBuilder.append(System.lineSeparator())
                      .append("export default { ")
                      .append(String.join(",", exportedSymbolNames))
                      .append(" }");

        return exportsBuilder.toString();
    }

    /**
     * Checks if is alphanumeric.
     *
     * @param str the str
     * @return true, if is alphanumeric
     */
    private boolean isAlphanumeric(String str) {
        if (str == null || str.equals("")) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * The Class ClassName.
     */
    private static class ClassName {

        /** The class name. */
        private final String className;

        /** The package and class name. */
        private final String packageAndClassName;

        /**
         * Instantiates a new class name.
         *
         * @param className the class name
         * @param packageAndClassName the package and class name
         */
        public ClassName(String className, String packageAndClassName) {
            this.className = className;
            this.packageAndClassName = packageAndClassName;
        }

        /**
         * Equals.
         *
         * @param o the o
         * @return true, if successful
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ClassName className1 = (ClassName) o;
            return Objects.equals(className, className1.className);
        }

        /**
         * Hash code.
         *
         * @return the int
         */
        @Override
        public int hashCode() {
            return Objects.hash(className);
        }
    }
}
