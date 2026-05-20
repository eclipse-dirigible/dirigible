/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.synchronizer;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * Reads raw source bytes for a registry-relative artefact location.
 *
 * <p>
 * Kept as a small static helper rather than a Spring bean so it can be invoked from the
 * synchronizer's reactive {@code complete} path without further plumbing — the underlying
 * {@link IRepository} is resolved lazily from the application context via {@link BeanProvider}.
 */
final class RegistrySourceLoader {

    private RegistrySourceLoader() {}

    /**
     * Read bytes for a source at the given registry-relative location.
     *
     * @param location relative to the project root (i.e. excludes {@code /registry/public/}).
     */
    static byte[] read(String location) {
        IResource resource = resolve(location);
        if (resource == null || !resource.exists()) {
            throw new IllegalStateException("Java source not found in registry: " + toFullPath(location));
        }
        return resource.getContent();
    }

    /** Does the source file still exist in the registry? */
    static boolean exists(String location) {
        IResource resource = resolve(location);
        return resource != null && resource.exists();
    }

    private static IResource resolve(String location) {
        IRepository repository = BeanProvider.getBean(IRepository.class);
        return repository.getResource(toFullPath(location));
    }

    private static String toFullPath(String location) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + (location.startsWith("/") ? location : "/" + location);
    }

}
