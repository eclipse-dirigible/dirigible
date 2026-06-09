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
import org.eclipse.dirigible.components.api.platform.RepositoryFacade;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * Mutable access to the Dirigible repository — resources (files), collections (folders), copies and
 * moves, content-typed creates and updates. Returns the platform's {@link IResource} /
 * {@link ICollection} domain types so callers can chain further operations (lock, version, set
 * properties).
 * <p>
 * The "repository" here is the on-disk Dirigible store (see {@code IRepository} /
 * {@code IRepositoryStructure}), <em>not</em> a JPA repository or a Git repository. For
 * workspace-scoped operations (per-user folders under {@code /users/<u>/workspace/<proj>}) prefer
 * {@link Workspace}; for the read-only public registry view prefer {@link Registry}.
 */
public final class Repository {

    private Repository() {}

    public static IResource getResource(String path) {
        return RepositoryFacade.getResource(path);
    }

    public static IResource createResource(String path, String content, String contentType) {
        return RepositoryFacade.createResource(path, content, contentType);
    }

    public static IResource createResourceNative(String path, byte[] content, String contentType) {
        return RepositoryFacade.createResourceNative(path, content, contentType);
    }

    public static IResource updateResource(String path, String content) {
        return RepositoryFacade.updateResource(path, content);
    }

    public static IResource updateResourceNative(String path, byte[] content) {
        return RepositoryFacade.updateResourceNative(path, content);
    }

    public static void deleteResource(String path) {
        RepositoryFacade.deleteResource(path);
    }

    public static ICollection getCollection(String path) {
        return RepositoryFacade.getCollection(path);
    }

    public static ICollection createCollection(String path) {
        return RepositoryFacade.createCollection(path);
    }

    public static void deleteCollection(String path) {
        RepositoryFacade.deleteCollection(path);
    }

    public static String find(String path, String pattern) throws IOException {
        return RepositoryFacade.find(path, pattern);
    }
}
