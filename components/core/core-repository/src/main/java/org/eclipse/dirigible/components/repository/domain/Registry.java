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

import java.util.ArrayList;
import java.util.List;

/**
 * The Registry transport object.
 */
public class Registry {

    /** The Constant TYPE_REGISTRY. */
    private static final String TYPE_REGISTRY = "registry";

    /** The name. */
    private String name;

    /** The path. */
    private String path;

    /** The type. */
    private String type = TYPE_REGISTRY;

    /** The collections. */
    private List<Collection> collections = new ArrayList<Collection>();

    /** The resources. */
    private List<Resource> resources = new ArrayList<Resource>();

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the new path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the collections.
     *
     * @return the collections
     */
    public List<Collection> getCollections() {
        return collections;
    }

    /**
     * Sets the collections.
     *
     * @param collections the new collections
     */
    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    /**
     * Gets the resources.
     *
     * @return the resources
     */
    public List<Resource> getResources() {
        return resources;
    }

    /**
     * Sets the resources.
     *
     * @param resources the new resources
     */
    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

}
