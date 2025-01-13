/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.javascript.modules;

import java.nio.file.Path;

/**
 * The Interface ModuleResolver.
 */
public interface ModuleResolver {

    /**
     * Checks if is resolvable.
     *
     * @param moduleToResolve the module to resolve
     * @return true, if is resolvable
     */
    boolean isResolvable(String moduleToResolve);

    /**
     * Resolve.
     *
     * @param moduleToResolve the module to resolve
     * @return the path
     */
    Path resolve(String moduleToResolve);
}
