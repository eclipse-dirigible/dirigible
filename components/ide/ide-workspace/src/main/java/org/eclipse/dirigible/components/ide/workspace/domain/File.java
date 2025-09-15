/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.domain;

import org.eclipse.dirigible.repository.api.*;

import java.io.InputStream;

/**
 * The Workspace's File.
 */
public class File implements IResource {

    /** The internal. */
    private final transient IResource internal;

    /**
     * Instantiates a new file.
     *
     * @param resource the resource
     */
    public File(IResource resource) {
        this.internal = resource;
    }

    /**
     * Gets the internal.
     *
     * @return the internal
     */
    public IResource getInternal() {
        return internal;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    @Override
    public IRepository getRepository() {
        return internal.getRepository();
    }

    /**
     * Gets the content.
     *
     * @return the content
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public byte[] getContent() throws RepositoryReadException {
        return internal.getContent();
    }

    /**
     * Sets the content.
     *
     * @param content the new content
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void setContent(byte[] content) throws RepositoryWriteException {
        internal.setContent(content);
    }

    @Override
    public InputStream getContentStream() throws RepositoryReadException {
        return internal.getContentStream();
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return internal.getName();
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    @Override
    public String getPath() {
        return internal.getPath();
    }

    /**
     * Sets the content.
     *
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void setContent(byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException {
        internal.setContent(content, isBinary, contentType);
    }

    /**
     * Gets the parent.
     *
     * @return the parent
     */
    @Override
    public ICollection getParent() {
        return internal.getParent();
    }

    /**
     * Gets the information.
     *
     * @return the information
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public IEntityInformation getInformation() throws RepositoryReadException {
        return internal.getInformation();
    }

    /**
     * Checks if is binary.
     *
     * @return true, if is binary
     */
    @Override
    public boolean isBinary() {
        return internal.isBinary();
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    @Override
    public String getContentType() {
        return internal.getContentType();
    }

    /**
     * Creates the.
     *
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void create() throws RepositoryWriteException {
        internal.create();
    }

    /**
     * Delete.
     *
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void delete() throws RepositoryWriteException {
        internal.delete();
    }

    /**
     * Rename to.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void renameTo(String name) throws RepositoryWriteException {
        internal.renameTo(name);
    }

    /**
     * Move to.
     *
     * @param path the path
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void moveTo(String path) throws RepositoryWriteException {
        internal.moveTo(path);
    }

    /**
     * Copy to.
     *
     * @param path the path
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void copyTo(String path) throws RepositoryWriteException {
        internal.copyTo(path);
    }

    /**
     * Exists.
     *
     * @return true, if successful
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public boolean exists() throws RepositoryReadException {
        return internal.exists();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public boolean isEmpty() throws RepositoryReadException {
        return internal.isEmpty();
    }

    /**
     * Gets the workspace path.
     *
     * @return the workspace path
     */
    public String getWorkspacePath() {
        RepositoryPath repositoryPath = new RepositoryPath(internal.getPath());
        return repositoryPath.constructPathFrom(2);
    }

    /**
     * Gets the project path.
     *
     * @return the project path
     */
    public String getProjectPath() {
        RepositoryPath repositoryPath = new RepositoryPath(internal.getPath());
        return repositoryPath.constructPathFrom(3);
    }
}
