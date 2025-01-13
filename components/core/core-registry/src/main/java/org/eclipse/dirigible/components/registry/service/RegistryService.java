/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.service;

import org.eclipse.dirigible.components.repository.domain.Registry;
import org.eclipse.dirigible.components.repository.domain.RepositoryJsonHelper;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Processing the Registry Service incoming requests.
 */
@Service
public class RegistryService {

    /** The Constant REGISTRY. */
    private static final String REGISTRY = "/registry";

    /** The repository. */
    private IRepository repository;

    /**
     * Instantiates a new registry service.
     *
     * @param repository the repository
     */
    @Autowired
    public RegistryService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    protected IRepository getRepository() {
        return repository;
    }

    /**
     * Gets the resource.
     *
     * @param path the path
     * @return the resource
     */
    public IResource getResource(String path) {
        StringBuilder registryPath = generateRegistryPath(path);
        return getRepository().getResource(registryPath.toString());
    }

    /**
     * Gets the collection.
     *
     * @param path the path
     * @return the collection
     */
    public ICollection getCollection(String path) {
        StringBuilder registryPath = generateRegistryPath(path);
        return getRepository().getCollection(registryPath.toString());
    }

    /**
     * Render registry.
     *
     * @param collection the collection
     * @return the registry
     */
    public Registry renderRegistry(ICollection collection) {
        return RepositoryJsonHelper.traverseRegistry(collection, IRepositoryStructure.PATH_REGISTRY_PUBLIC, REGISTRY);
    }

    /**
     * Generate registry path.
     *
     * @param path the path
     * @return the string builder
     */
    private StringBuilder generateRegistryPath(String path) {
        StringBuilder registryPath = new StringBuilder(IRepositoryStructure.PATH_REGISTRY_PUBLIC).append(IRepositoryStructure.SEPARATOR)
                                                                                                 .append(path);
        return registryPath;
    }

}
