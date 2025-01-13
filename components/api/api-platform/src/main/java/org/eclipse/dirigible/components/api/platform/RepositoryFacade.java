/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.io.IOException;

import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Repository Facade.
 */
@Component
public class RepositoryFacade implements InitializingBean {

    /** The instance. */
    private static RepositoryFacade INSTANCE;

    /** The repository. */
    private IRepository repository;

    /**
     * Instantiates a new repository facade.
     *
     * @param repository the repository
     */
    @Autowired
    private RepositoryFacade(IRepository repository) {
        this.repository = repository;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static RepositoryFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }


    /**
     * Gets the resource.
     *
     * @param path the path
     * @return the resource
     */
    public static IResource getResource(String path) {
        return RepositoryFacade.get()
                               .getRepository()
                               .getResource(path);
    }

    /**
     * Creates the resource.
     *
     * @param path the path
     * @param content the content
     * @param contentType the content type
     * @return the resource
     */
    public static IResource createResource(String path, String content, String contentType) {
        return RepositoryFacade.get()
                               .getRepository()
                               .createResource(path, content.getBytes(), ContentTypeHelper.isBinary(contentType), contentType);
    }

    /**
     * Creates the resource.
     *
     * @param path the path
     * @param content the content
     * @param contentType the content type
     * @return the resource
     */
    public static IResource createResourceNative(String path, byte[] content, String contentType) {
        return RepositoryFacade.get()
                               .getRepository()
                               .createResource(path, content, ContentTypeHelper.isBinary(contentType), contentType);
    }

    /**
     * Update resource.
     *
     * @param path the path
     * @param content the content
     * @return the resource
     */
    public static IResource updateResource(String path, String content) {
        IResource resource = RepositoryFacade.get()
                                             .getRepository()
                                             .getResource(path);
        resource.setContent(content.getBytes());
        return resource;
    }

    /**
     * Update resource.
     *
     * @param path the path
     * @param content the content
     * @return the resource
     */
    public static IResource updateResourceNative(String path, byte[] content) {
        return RepositoryFacade.updateResource(path, path);
    }

    /**
     * Deletes a resource.
     *
     * @param path the path
     */
    public static void deleteResource(String path) {
        RepositoryFacade.get()
                        .getRepository()
                        .getResource(path)
                        .delete();
    }

    /**
     * Gets the collection.
     *
     * @param path the path
     * @return the collection
     */
    public static ICollection getCollection(String path) {
        return RepositoryFacade.get()
                               .getRepository()
                               .getCollection(path);
    }

    /**
     * Creates a new collection.
     *
     * @param path the path
     * @return the collection
     */
    public static ICollection createCollection(String path) {
        return RepositoryFacade.get()
                               .getRepository()
                               .createCollection(path);
    }

    /**
     * Deletes a collection.
     *
     * @param path the path
     */
    public static void deleteCollection(String path) {
        RepositoryFacade.get()
                        .getRepository()
                        .getCollection(path)
                        .delete();
    }

    /**
     * Find all the files matching the pattern.
     *
     * @param path the root path
     * @param pattern the glob pattern
     * @return the list of file names
     * @throws IOException in case of an error
     */
    public static String find(String path, String pattern) throws IOException {
        return GsonHelper.toJson(RepositoryFacade.get()
                                                 .getRepository()
                                                 .find(path, pattern));
    }
}
