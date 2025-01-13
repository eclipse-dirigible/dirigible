/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import java.util.Date;

import org.eclipse.dirigible.repository.fs.FileSystemRepository;

/**
 * The base of domain objects in the file system based implementation.
 */
public abstract class LocalObject {

    /** The repository. */
    private FileSystemRepository repository;

    /** The name. */
    private String name;

    /** The path. */
    private String path;

    /** The permissions. */
    private int permissions;

    /** The created by. */
    private String createdBy;

    /** The created at. */
    private Date createdAt;

    /** The modified by. */
    private String modifiedBy;

    /** The modified at. */
    private Date modifiedAt;

    /**
     * Instantiates a new local object.
     *
     * @param repository the repository
     */
    public LocalObject(FileSystemRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public FileSystemRepository getRepository() {
        return repository;
    }

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
     * Gets the permissions.
     *
     * @return the permissions
     */
    public int getPermissions() {
        return permissions;
    }

    /**
     * Sets the permissions.
     *
     * @param permissions the new permissions
     */
    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    /**
     * Gets the created by.
     *
     * @return the created by
     */
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * Sets the created by.
     *
     * @param createdBy the new created by
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Gets the created at.
     *
     * @return the created at
     */
    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the new created at
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the modified by.
     *
     * @return the modified by
     */
    public String getModifiedBy() {
        return this.modifiedBy;
    }

    /**
     * Sets the modified by.
     *
     * @param modifiedBy the new modified by
     */
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * Gets the modified at.
     *
     * @return the modified at
     */
    public Date getModifiedAt() {
        return this.modifiedAt;
    }

    /**
     * Sets the modified at.
     *
     * @param modifiedAt the new modified at
     */
    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

}
