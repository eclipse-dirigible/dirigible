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

import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class JavaModuleResolver.
 */
public class JavaModuleResolver implements ModuleResolver {

    /** The Constant JAVA_MODULE_PATTERN. */
    private static final Pattern JAVA_MODULE_PATTERN = Pattern.compile("(@java)(\\/)(.+[^\"])");

    /** The cache directory path. */
    private final Path cacheDirectoryPath;

    /** The java package proxy generator. */
    private final JavaPackageProxyGenerator javaPackageProxyGenerator;

    /**
     * Instantiates a new java module resolver.
     *
     * @param cacheDirectoryPath the cache directory path
     */
    public JavaModuleResolver(Path cacheDirectoryPath) {
        javaPackageProxyGenerator = new JavaPackageProxyGenerator();
        cacheDirectoryPath.toFile()
                          .mkdirs();
        this.cacheDirectoryPath = cacheDirectoryPath;
    }

    /**
     * Checks if is resolvable.
     *
     * @param moduleToResolve the module to resolve
     * @return true, if is resolvable
     */
    @Override
    public boolean isResolvable(String moduleToResolve) {
        return moduleToResolve.contains("@java");
    }

    /**
     * Resolve.
     *
     * @param moduleToResolve the module to resolve
     * @return the path
     */
    @Override
    public Path resolve(String moduleToResolve) {
        Matcher modulePathMatcher = JAVA_MODULE_PATTERN.matcher(moduleToResolve);
        if (!modulePathMatcher.matches()) {
            throw new RuntimeException("Found invalid Java module path!");
        }

        String javaPackageName = modulePathMatcher.group(3);

        Path javaPackageProxyGeneratedPath = cacheDirectoryPath.resolve(javaPackageName + ".mjs");
        File javaPackageProxyGeneratedFile = javaPackageProxyGeneratedPath.toFile();

        if (javaPackageProxyGeneratedFile.exists()) {
            return javaPackageProxyGeneratedPath;
        }

        String coreModuleContent = javaPackageProxyGenerator.generate(javaPackageName);
        try {
            Files.writeString(javaPackageProxyGeneratedPath, coreModuleContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return javaPackageProxyGeneratedPath;
    }
}
