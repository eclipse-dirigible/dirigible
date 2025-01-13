/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local.module;

import java.util.List;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IMasterRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryInitializationException;
import org.eclipse.dirigible.repository.api.RepositoryReadException;

/**
 * The Class DummyMasterRepository.
 */
public class DummyMasterRepository implements IMasterRepository {

    /**
     * Initialize.
     *
     * @throws RepositoryInitializationException the repository initialization exception
     */
    @Override
    public void initialize() throws RepositoryInitializationException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the root.
     *
     * @return the root
     */
    @Override
    public ICollection getRoot() {
        return null;
    }

    /**
     * Gets the collection.
     *
     * @param path the path
     * @return the collection
     */
    @Override
    public ICollection getCollection(String path) {
        return null;
    }

    /**
     * Checks for collection.
     *
     * @param path the path
     * @return true, if successful
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public boolean hasCollection(String path) throws RepositoryReadException {
        return false;
    }

    /**
     * Gets the resource.
     *
     * @param path the path
     * @return the resource
     */
    @Override
    public IResource getResource(String path) {
        return null;
    }

    /**
     * Checks for resource.
     *
     * @param path the path
     * @return true, if successful
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public boolean hasResource(String path) throws RepositoryReadException {
        return false;
    }

    /**
     * Gets the all resource paths.
     *
     * @return the all resource paths
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<String> getAllResourcePaths() throws RepositoryReadException {
        return null;
    }

}
