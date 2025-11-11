/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.modules;

import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleResolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class DirigibleModuleResolver.
 */
public class DirigibleModuleResolver implements ModuleResolver {

    /** The Constant DIRIGIBLE_CORE_MODULE_SIGNATURE. */
    private static final String DIRIGIBLE_CORE_MODULE_SIGNATURE = "sdk";

    /** The Constant DIRIGIBLE_CORE_MODULE_SIGNATURE_PATTERN. */
    private static final Pattern DIRIGIBLE_CORE_MODULE_SIGNATURE_PATTERN = Pattern.compile("((?:@aerokit/)?sdk)(\\/)(\\w+)"); // e.g.
    // sdk/core
    // => $1=sdk # Captures either sdk or @aerokit/sdk
    // $2=/ $3=core

    /** The dirigible module ESM proxy generator. */
    private final DirigibleModuleESMProxyGenerator dirigibleModuleESMProxyGenerator;

    /** The cache directory path. */
    private final Path cacheDirectoryPath;

    /**
     * Instantiates a new dirigible module resolver.
     *
     * @param cacheDirectoryPath the cache directory path
     * @param dirigibleSourceProvider the dirigible source provider
     */
    public DirigibleModuleResolver(Path cacheDirectoryPath, JavascriptSourceProvider dirigibleSourceProvider) {
        dirigibleModuleESMProxyGenerator = new DirigibleModuleESMProxyGenerator(dirigibleSourceProvider);
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
        return moduleToResolve.contains(DIRIGIBLE_CORE_MODULE_SIGNATURE) && !DirigibleModulesMetadata.isPureEsmModule(moduleToResolve);
    }

    /**
     * Resolve.
     *
     * @param moduleToResolve the module to resolve
     * @return the path
     */
    @Override
    public Path resolve(String moduleToResolve) {
        Matcher modulePathMatcher = DIRIGIBLE_CORE_MODULE_SIGNATURE_PATTERN.matcher(moduleToResolve);
        if (!modulePathMatcher.matches()) {
            throw new RuntimeException("Found invalid Dirigible core modules path: " + moduleToResolve);
        }

        String coreModuleName = modulePathMatcher.group(3);

        Path coreModuleGeneratedPath = cacheDirectoryPath.resolve(coreModuleName + ".mjs");
        File coreModuleGeneratedFile = coreModuleGeneratedPath.toFile();

        if (coreModuleGeneratedFile.exists()) {
            return coreModuleGeneratedFile.toPath();
        }

        String coreModuleContent = dirigibleModuleESMProxyGenerator.generate(coreModuleName, "");
        try {
            Files.writeString(coreModuleGeneratedPath, coreModuleContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return coreModuleGeneratedFile.toPath();
    }
}
