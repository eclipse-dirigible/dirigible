/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;

import java.io.InputStream;
import java.util.List;

/**
 * Internal representation of a Folder/Collection kind of object.
 */
public class LocalFolder extends LocalObject {

    /**
     * Instantiates a new local folder.
     *
     * @param repository the repository
     */
    public LocalFolder(FileSystemRepository repository) {
        super(repository);
    }

    /**
     * Delete tree.
     *
     * @throws LocalRepositoryException the local repository exception
     */
    public void deleteTree() throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .removeFolderByPath(getPath());
    }

    /**
     * Gets the children.
     *
     * @return the children
     * @throws LocalRepositoryException the local repository exception
     */
    public List<LocalObject> getChildren() throws LocalRepositoryException {
        List<LocalObject> result = getRepository().getRepositoryDao()
                                                  .getChildrenByFolder(getPath());
        return result;
    }

    /**
     * Creates the folder.
     *
     * @param name the name
     * @throws LocalRepositoryException the local repository exception
     */
    public void createFolder(String name) throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .createFolder(RepositoryPath.normalizePath(getPath(), name));
    }

    /**
     * Creates the file.
     *
     * @param name the name
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @throws LocalRepositoryException the local repository exception
     */
    public void createFile(String name, byte[] content, boolean isBinary, String contentType) throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .createFile(RepositoryPath.normalizePath(getPath(), name), content, isBinary, contentType);
    }

    public void createFile(String name, InputStream contentInputStream) throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .createFile(RepositoryPath.normalizePath(getPath(), name), contentInputStream);
    }

    /**
     * Rename folder.
     *
     * @param newPath the new path
     * @throws LocalRepositoryException the local repository exception
     */
    public void renameFolder(String newPath) throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .renameFolder(getPath(), newPath);
    }

    /**
     * Copy folder.
     *
     * @param newPath the new path
     * @throws LocalRepositoryException the local repository exception
     */
    public void copyFolder(String newPath) throws LocalRepositoryException {
        getRepository().getRepositoryDao()
                       .copyFolder(getPath(), newPath);
    }

}
