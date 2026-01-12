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

import java.nio.file.Path;

import org.eclipse.dirigible.graalium.core.JavascriptSourceProvider;
import org.eclipse.dirigible.graalium.core.javascript.modules.ModuleResolver;

/**
 * The Class DirigibleGlobalModuleResolver.
 */
public class DirigibleGlobalModuleResolver implements ModuleResolver {

    /** The source provider. */
    private final JavascriptSourceProvider sourceProvider;

    /**
     * Instantiates a new dirigible global module resolver.
     *
     * @param sourceProvider the source provider
     */
    public DirigibleGlobalModuleResolver(JavascriptSourceProvider sourceProvider) {
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
        return !moduleToResolve.startsWith("@aerokit/sdk/") && !Path.of(moduleToResolve)
                                                                    .isAbsolute();
    }

    /**
     * Resolve.
     *
     * @param moduleToResolve the module to resolve
     * @return the path
     */
    @Override
    public Path resolve(String moduleToResolve) {
        return sourceProvider.getAbsoluteProjectPath("")
                             .resolve(moduleToResolve + ".js");
    }
}
