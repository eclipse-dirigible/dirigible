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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The Workspace's Folder.
 */
public class Folder implements ICollection {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(Folder.class);

    /** The internal. */
    private final transient ICollection internal;

    /**
     * Instantiates a new folder.
     *
     * @param collection the collection
     */
    public Folder(ICollection collection) {
        this.internal = collection;
    }

    /**
     * Gets the internal.
     *
     * @return the internal
     */
    public ICollection getInternal() {
        return internal;
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
     * Gets the collections names.
     *
     * @return the collections names
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<String> getCollectionsNames() throws RepositoryReadException {
        return internal.getCollectionsNames();
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
     * Creates the collection.
     *
     * @param name the name
     * @return the i collection
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public ICollection createCollection(String name) throws RepositoryReadException {
        return internal.createCollection(name);
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
     * Gets the collection.
     *
     * @param name the name
     * @return the collection
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public ICollection getCollection(String name) throws RepositoryReadException {
        return internal.getCollection(name);
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
     * Removes the collection.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void removeCollection(String name) throws RepositoryWriteException {
        internal.removeCollection(name);
    }

    /**
     * Removes the collection.
     *
     * @param collection the collection
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void removeCollection(ICollection collection) throws RepositoryWriteException {
        collection.removeCollection(collection);
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
     * Gets the resources names.
     *
     * @return the resources names
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<String> getResourcesNames() throws RepositoryReadException {
        return internal.getResourcesNames();
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
     * Gets the resource.
     *
     * @param name the name
     * @return the resource
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public IResource getResource(String name) throws RepositoryReadException {
        return internal.getResource(name);
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
     * Removes the resource.
     *
     * @param name the name
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void removeResource(String name) throws RepositoryWriteException {
        internal.removeResource(name);
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
     * Removes the resource.
     *
     * @param resource the resource
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public void removeResource(IResource resource) throws RepositoryWriteException {
        internal.removeResource(resource);
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
     * Gets the children.
     *
     * @return the children
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<IEntity> getChildren() throws RepositoryReadException {
        return internal.getChildren();
    }

    /**
     * Creates the resource.
     *
     * @param name the name
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @return the i resource
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public IResource createResource(String name, byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException {
        return internal.createResource(name, content, isBinary, contentType);
    }

    @Override
    public IResource createResource(String name, InputStream contentInputStream, boolean isBinary, String contentType)
            throws RepositoryWriteException {
        return internal.createResource(name, contentInputStream, isBinary, contentType);
    }

    /**
     * Creates the resource.
     *
     * @param name the name
     * @param content the content
     * @return the i resource
     * @throws RepositoryWriteException the repository write exception
     */
    @Override
    public IResource createResource(String name, byte[] content) throws RepositoryWriteException {
        return internal.createResource(name, content);
    }

    /**
     * Creates the folder.
     *
     * @param path the path
     * @return the folder
     */
    public Folder createFolder(String path) {
        String fullPath = constructPath(path);
        ICollection collection = this.getRepository()
                                     .createCollection(fullPath);
        return new Folder(collection);
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
     * Construct path.
     *
     * @param path the path
     * @return the string
     */
    protected String constructPath(String path) {
        return new RepositoryPath(this.getPath(), path).build();
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
     * Gets the folder.
     *
     * @param path the path
     * @return the folder
     */
    public Folder getFolder(String path) {
        String fullPath = constructPath(path);
        ICollection collection = this.getRepository()
                                     .getCollection(fullPath);
        return new Folder(collection);
    }

    /**
     * Exists folder.
     *
     * @param path the path
     * @return true, if successful
     */
    public boolean existsFolder(String path) {
        String fullPath = constructPath(path);
        ICollection collection = this.getRepository()
                                     .getCollection(fullPath);
        return collection.exists();
    }

    /**
     * Gets the folders.
     *
     * @return the folders
     */
    public List<Folder> getFolders() {
        List<Folder> folders = new ArrayList<Folder>();
        List<ICollection> collections = this.getCollections();
        for (ICollection collection : collections) {
            folders.add(new Folder(collection));
        }
        return folders;
    }

    /**
     * Gets the collections.
     *
     * @return the collections
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<ICollection> getCollections() throws RepositoryReadException {
        return internal.getCollections();
    }

    /**
     * Delete folder.
     *
     * @param path the path
     */
    public void deleteFolder(String path) {
        String fullPath = constructPath(path);
        this.getRepository()
            .removeCollection(fullPath);
    }

    /**
     * Creates the file.
     *
     * @param path the path
     * @param content the content
     * @return the file
     */
    public File createFile(String path, byte[] content) {
        String fullPath = constructPath(path);
        IResource resource = this.getRepository()
                                 .createResource(fullPath, content);
        return new File(resource);
    }

    /**
     * Creates the file.
     *
     * @param path the path
     * @param content the content
     * @param isBinary the is binary
     * @param contentType the content type
     * @return the file
     */
    public File createFile(String path, byte[] content, boolean isBinary, String contentType) {
        String fullPath = constructPath(path);
        IResource resource = this.getRepository()
                                 .createResource(fullPath, content, isBinary, contentType);
        return new File(resource);
    }

    /**
     * Gets the file.
     *
     * @param path the path
     * @return the file
     */
    public File getFile(String path) {
        String fullPath = constructPath(path);
        IResource resource = this.getRepository()
                                 .getResource(fullPath);
        return new File(resource);
    }

    /**
     * Exists file.
     *
     * @param path the path
     * @return true, if successful
     */
    public boolean existsFile(String path) {
        String fullPath = constructPath(path);
        IResource resource = this.getRepository()
                                 .getResource(fullPath);
        return resource.exists();
    }

    /**
     * Gets the files.
     *
     * @return the files
     */
    public List<File> getFiles() {
        List<File> files = new ArrayList<File>();
        List<IResource> resources = this.getResources();
        for (IResource resource : resources) {
            files.add(new File(resource));
        }
        return files;
    }

    /**
     * Gets the resources.
     *
     * @return the resources
     * @throws RepositoryReadException the repository read exception
     */
    @Override
    public List<IResource> getResources() throws RepositoryReadException {
        return internal.getResources();
    }

    /**
     * Delete file.
     *
     * @param path the path
     */
    public void deleteFile(String path) {
        String fullPath = constructPath(path);
        this.getRepository()
            .removeResource(fullPath);
    }

    /**
     * Search.
     *
     * @param term the term
     * @return the list
     */
    public List<File> search(String term) {
        List<File> files = new ArrayList<File>();
        try {
            List<IEntity> entities = this.getRepository()
                                         .searchText(term);
            for (IEntity entity : entities) {
                if (entity instanceof IResource resource) {
                    if (resource.getPath()
                                .startsWith(this.getPath())) {
                        if (resource.exists()) {
                            files.add(new File(resource));
                        }
                    }
                }
            }
        } catch (RepositorySearchException | RepositoryReadException e) {
            logger.warn(e.getMessage(), e);
        }
        return files;
    }

    /**
     * Find.
     *
     * @param pattern the pattern
     * @return the list
     */
    public List<File> find(String pattern) {
        List<File> files = new ArrayList<File>();
        try {
            List<String> entities = this.getRepository()
                                        .find(this.getPath(), pattern);

            for (String entity : entities) {
                IResource resource = this.getRepository()
                                         .getResource(entity);
                if (resource.getPath()
                            .startsWith(this.getPath())) {
                    if (resource.exists()) {
                        files.add(new File(resource));
                    }
                }
            }
        } catch (RepositorySearchException | RepositoryReadException e) {
            logger.warn(e.getMessage(), e);
        }
        return files;
    }

}
