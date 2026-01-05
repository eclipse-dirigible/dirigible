/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

import java.io.InputStream;
import java.util.List;

/**
 * The <code>ICollection</code> interface represents a collection in the repository.
 */
public interface ICollection extends IEntity {

    /**
     * Returns a list of all the child collections held by this collection.
     *
     * @return a list of {@link ICollection} instances
     * @throws RepositoryReadException in case the list of {@link ICollection} cannot be retrieved
     */
    List<ICollection> getCollections() throws RepositoryReadException;

    /**
     * Returns a list containing the names of all the child collections directly contained within this
     * collection.
     *
     * @return a list of {@link ICollection} instances names
     * @throws RepositoryReadException in case the names cannot be retrieved
     */
    List<String> getCollectionsNames() throws RepositoryReadException;

    /**
     * Creates a new collection with the specified name in this collection.
     * <p>
     * The name should not contain any slashes.
     * <p>
     * The change is persisted to the backend.
     *
     * @param name the {@link ICollection} name
     * @return an {@link ICollection} instance
     * @throws RepositoryReadException in the {@link ICollection} cannot be created
     */
    ICollection createCollection(String name) throws RepositoryReadException;

    /**
     * Returns the collection with the specified name contained in this collection.
     * <p>
     * The returned collection is just a representation. It may not exist on the backend.
     *
     * @param name the name of the collection
     * @return an {@link ICollection} instance.
     * @throws RepositoryReadException in case the {@link ICollection} cannot be retrieved
     */
    ICollection getCollection(String name) throws RepositoryReadException;

    /**
     * Removes the collection with the specified name contained in this collection.
     * <p>
     * The name should not contain any slashes.
     * <p>
     * The change is persisted to the backend.
     *
     * @param name the name of the {@link ICollection} to be removed
     * @throws RepositoryWriteException in case the {@link ICollection}cannot be removed
     */
    void removeCollection(String name) throws RepositoryWriteException;

    /**
     * Removes the child collection represented by the parameter.
     *
     * @param collection the {@link ICollection} instance to be removed
     * @throws RepositoryWriteException in case the {@link ICollection} cannot be removed
     */
    void removeCollection(ICollection collection) throws RepositoryWriteException;

    /**
     * Returns a list of all the resources held by this collection.
     *
     * @return a list of {@link IResource} instances
     * @throws RepositoryReadException in case the list of {@link IResource} cannot be retrieved
     */
    List<IResource> getResources() throws RepositoryReadException;

    /**
     * Returns a list containing the names of all the resources directly contained in this collection.
     *
     * @return a list of {@link IResource} instances names
     * @throws RepositoryReadException in case the list of {@link IResource} names cannot be retrieved
     */
    List<String> getResourcesNames() throws RepositoryReadException;

    /**
     * Returns the resource with the specified name contained in this collection.
     * <p>
     * The returned resource is just a representation. It may not exist on the backend.
     *
     * @param name the name of the {@link IResource}
     * @return a {@link IResource} instance
     * @throws RepositoryReadException in case the {@link IResource} cannot be retrieved
     */
    IResource getResource(String name) throws RepositoryReadException;

    /**
     * Removes the resource with the specified name from this collection.
     * <p>
     * Changes are persisted to the backend.
     *
     * @param name the name of the {@link IResource} instance to be removed
     * @throws RepositoryWriteException in case the {@link IResource} cannot be removed
     */
    void removeResource(String name) throws RepositoryWriteException;

    /**
     * Removes the child resource represented by the parameter.
     * <p>
     * Changes are persisted to the backend.
     *
     * @param resource the {@link IResource} to be removed
     * @throws RepositoryWriteException in case the {@link IResource} cannot be removed
     */
    void removeResource(IResource resource) throws RepositoryWriteException;

    /**
     * List the children of this collection.
     *
     * @return a list of {@link IEntity} instances
     * @throws RepositoryReadException in case the list of the {@link IEntity} cannot be retrieved
     */
    List<IEntity> getChildren() throws RepositoryReadException;

    /**
     * Create resource under this collection by specifying the binary flag.
     *
     * @param name the name of the {@link IResource}
     * @param content the raw content
     * @param isBinary whether it is binary
     * @param contentType the type of the content
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case the {@link IResource} cannot be created
     */
    IResource createResource(String name, byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException;

    IResource createResource(String name, InputStream contentInputStream, boolean isBinary, String contentType)
            throws RepositoryWriteException;

    /**
     * Create resource under this collection by recognizing the content type and binary flag by its file
     * extension.
     *
     * @param name the name of the {@link IResource}
     * @param content the raw content
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case the {@link IResource} cannot be created
     */
    IResource createResource(String name, byte[] content) throws RepositoryWriteException;

}
