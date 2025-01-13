/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

import java.io.IOException;

/**
 * This interface represents a Repository. It allows for querying, modifying and navigating through
 * collections and resources.
 */
public interface IRepositoryWriter {

    /**
     * This method creates a new empty collection at the specified path.
     * <p>
     * The returned value is an instance of <code>ICollection</code> which represents the newly created
     * collection.
     *
     * @param path {@link ICollection} location
     * @return an {@link ICollection} instance
     * @throws RepositoryWriteException in case the {@link ICollection} cannot be created
     */
    public ICollection createCollection(String path) throws RepositoryWriteException;

    /**
     * This method removes the collection with the specified path from the repository.
     *
     * @param path the {@link ICollection} location
     * @throws RepositoryWriteException in case the {@link ICollection} cannot be removed
     */
    public void removeCollection(String path) throws RepositoryWriteException;

    /**
     * This method creates a new empty resource at the specified path.
     * <p>
     * The returned value is an instance of <code>IResource</code> that represents the newly created
     * resource.
     *
     * @param path the {@link IResource} location
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case {@link IResource} cannot be created
     */
    public IResource createResource(String path) throws RepositoryWriteException;

    /**
     * This method creates a new resource at the specified path and fills it with the specified content.
     * <p>
     * The returned value is an instance of <code>IResource</code> that represents the newly created
     * resource.
     *
     * @param path the {@link IResource} location
     * @param content the raw content
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case {@link IResource} cannot be created
     */
    public IResource createResource(String path, byte[] content) throws RepositoryWriteException;

    /**
     * This method creates a new empty resource at the specified path.
     * <p>
     * The returned value is an instance of <code>IResource</code> that represents the newly created
     * resource.
     *
     * @param path the {@link IResource} location
     * @param content the raw content
     * @param isBinary whether it is binary
     * @param contentType the type of the content
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case the {@link IResource} cannot be created
     */
    public IResource createResource(String path, byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException;

    /**
     * This method creates a new empty, or override resource at the specified path.
     * <p>
     * The returned value is an instance of <code>IResource</code> that represents the newly created
     * resource.
     *
     * @param path the {@link IResource} location
     * @param content the raw content
     * @param isBinary whether it is binary
     * @param contentType the type of the content
     * @param override whether to override existing
     * @return an {@link IResource} instance
     * @throws RepositoryWriteException in case the {@link IResource} cannot be created
     */
    public IResource createResource(String path, byte[] content, boolean isBinary, String contentType, boolean override)
            throws RepositoryWriteException;

    /**
     * This method removes the resource at the specified path from the repository.
     *
     * @param path the {@link IResource} location
     * @throws RepositoryWriteException in case the {@link IResource} cannot be removed
     */
    public void removeResource(String path) throws RepositoryWriteException;

    /**
     * Disposes of this repository.
     * <p>
     * Calling this method allows for the repository to release all allocated resources.
     * <p>
     * Calling this method more than once will be a no-op.
     */
    public void dispose();

    /**
     * Whether the Repository supports linking of external paths.
     *
     * @return true, if is linking paths supported
     */
    public boolean isLinkingPathsSupported();

    /**
     * Link external folder or file as an internal Repository artifact.
     *
     * @param repositoryPath the relative path
     * @param filePath the target folder or file
     * @throws IOException in case of exception
     */
    public void linkPath(String repositoryPath, String filePath) throws IOException;

    /**
     * Delete link together with external folder or file.
     *
     * @param repositoryPath the relative path
     * @throws IOException in case of exception
     */
    public void deleteLinkedPath(String repositoryPath) throws IOException;

    /**
     * Check if Link exists between folder or file and an internal Repository artifact.
     *
     * @param repositoryPath the relative path
     * @return true if link exists
     */
    public boolean isLinkedPath(String repositoryPath);

}
