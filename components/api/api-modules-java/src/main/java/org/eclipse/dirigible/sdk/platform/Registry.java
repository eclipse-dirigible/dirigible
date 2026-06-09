/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

import java.io.IOException;
import org.eclipse.dirigible.components.api.platform.RegistryFacade;

/**
 * Read-only access to artefacts published under {@code /registry/public/}. Use this when a
 * controller, job, or extension needs to consume content that ships with another project —
 * configuration files, fixed templates, lookup tables — without going through HTTP or a custom
 * service.
 * <p>
 * For mutable access (creating, updating, deleting resources or whole collections) use
 * {@link Repository}; for IDE workspace operations use
 * {@link org.eclipse.dirigible.sdk.platform.Workspace Workspace}.
 */
public final class Registry {

    private Registry() {}

    public static byte[] getContent(String path) throws IOException {
        return RegistryFacade.getContent(path);
    }

    public static String getText(String path) throws IOException {
        return RegistryFacade.getText(path);
    }

    public static boolean exists(String path) throws IOException {
        return RegistryFacade.exists(path);
    }

    public static String toRepositoryPath(String path) {
        return RegistryFacade.toRepositoryPath(path);
    }

    public static String toRegistryPath(String path) {
        return RegistryFacade.toRegistryPath(path);
    }

    public static String toResourcePath(String path) {
        return RegistryFacade.toResourcePath(path);
    }

    public static String find(String path, String pattern) throws IOException {
        return RegistryFacade.find(path, pattern);
    }
}
