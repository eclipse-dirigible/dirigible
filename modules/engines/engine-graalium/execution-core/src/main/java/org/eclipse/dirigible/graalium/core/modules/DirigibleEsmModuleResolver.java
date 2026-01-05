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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleResolver;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class DirigibleEsmModuleResolver.
 */
public class DirigibleEsmModuleResolver implements ModuleResolver {

    /** The Constant DIRIGIBLE_CORE_MODULE_SIGNATURE_PATTERN. */
    private static final Pattern DIRIGIBLE_CORE_MODULE_SIGNATURE_PATTERN = Pattern.compile("((?:@aerokit/)?sdk)/(\\w+)(?:/(.+))?"); // e.g.
    // sdk/core/module/submodule
    // =>
    // $1=sdk # Captures either sdk or @aerokit/sdk
    // $2=core
    // $3=module/submodule

    /** The source provider. */
    private final JavascriptSourceProvider sourceProvider;

    /**
     * Instantiates a new dirigible esm module resolver.
     *
     * @param sourceProvider the source provider
     */
    public DirigibleEsmModuleResolver(JavascriptSourceProvider sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    /**
     * Checks if is resolvable.
     *
     * @param moduleToResolve the module to resolve
     * @return true, if is resolvable
     */
    @Override
    public boolean isResolvable(String moduleToResolve) {
        return moduleToResolve.contains("sdk") && DirigibleModulesMetadata.isPureEsmModule(moduleToResolve);
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

        String dirigibleModuleDir = modulePathMatcher.group(2);
        String dirigibleModuleFile = modulePathMatcher.group(3);

        boolean hasDirigibleModuleFile = !StringUtils.isEmpty(dirigibleModuleFile);

        Path dirigibleModulePath = sourceProvider.getAbsoluteProjectPath("modules")
                                                 .resolve("dist")
                                                 .resolve("esm")
                                                 .resolve(dirigibleModuleDir)
                                                 .resolve(hasDirigibleModuleFile ? dirigibleModuleFile + ".mjs" : "index.mjs");
        return dirigibleModulePath;
    }
}
