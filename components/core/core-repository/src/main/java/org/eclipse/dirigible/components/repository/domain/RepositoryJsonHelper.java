/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.repository.domain;

import java.util.List;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * The Repository JSON Helper.
 */
public class RepositoryJsonHelper {

    /**
     * Traverse repository.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the repository
     */
    public static Repository traverseRepository(ICollection collection, String removePathPrefix, String addPathPrefix) {
        Repository repositoryPojo = new Repository();
        repositoryPojo.setName(collection.getName());
        repositoryPojo.setPath(addPathPrefix + collection.getPath()
                                                         .substring(removePathPrefix.length()));
        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            repositoryPojo.getCollections()
                          .add(traverseCollection(childCollection, removePathPrefix, addPathPrefix));
        }

        List<IResource> resources = collection.getResources();
        for (IResource childResource : resources) {
            Resource resourcePojo = new Resource();
            resourcePojo.setName(childResource.getName());
            resourcePojo.setPath(addPathPrefix + childResource.getPath()
                                                              .substring(removePathPrefix.length()));
            resourcePojo.setContentType(childResource.getContentType());
            repositoryPojo.getResources()
                          .add(resourcePojo);
        }

        return repositoryPojo;
    }

    /**
     * Traverse registry.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the registry
     */
    public static Registry traverseRegistry(ICollection collection, String removePathPrefix, String addPathPrefix) {
        Registry registryPojo = new Registry();
        registryPojo.setName(collection.getName());
        registryPojo.setPath(addPathPrefix + collection.getPath()
                                                       .substring(removePathPrefix.length()));
        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            registryPojo.getCollections()
                        .add(traverseCollection(childCollection, removePathPrefix, addPathPrefix));
        }

        List<IResource> resources = collection.getResources();
        for (IResource childResource : resources) {
            Resource resourcePojo = new Resource();
            resourcePojo.setName(childResource.getName());
            resourcePojo.setPath(addPathPrefix + childResource.getPath()
                                                              .substring(removePathPrefix.length()));
            resourcePojo.setContentType(childResource.getContentType());
            registryPojo.getResources()
                        .add(resourcePojo);
        }

        return registryPojo;
    }

    /**
     * Traverse collection.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the collection
     */
    public static Collection traverseCollection(ICollection collection, String removePathPrefix, String addPathPrefix) {
        Collection collectionPojo = new Collection();
        collectionPojo.setName(collection.getName());
        collectionPojo.setPath(addPathPrefix + collection.getPath()
                                                         .substring(removePathPrefix.length()));
        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            collectionPojo.getCollections()
                          .add(traverseCollection(childCollection, removePathPrefix, addPathPrefix));
        }

        List<IResource> resources = collection.getResources();
        for (IResource childResource : resources) {
            Resource resourcePojo = new Resource();
            resourcePojo.setName(childResource.getName());
            resourcePojo.setPath(addPathPrefix + childResource.getPath()
                                                              .substring(removePathPrefix.length()));
            resourcePojo.setContentType(childResource.getContentType());
            collectionPojo.getResources()
                          .add(resourcePojo);
        }

        return collectionPojo;
    }

}
