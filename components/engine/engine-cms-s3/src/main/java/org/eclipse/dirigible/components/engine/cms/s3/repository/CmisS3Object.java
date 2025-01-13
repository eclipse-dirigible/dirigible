/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.eclipse.dirigible.components.api.s3.S3Facade;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.ObjectType;

import java.io.IOException;

/**
 * The Class CmisS3Object.
 */
public class CmisS3Object implements CmisObject {

    /**
     * The id/path of the object.
     */
    private final String id;
    /** The name. */
    private final String name;
    /**
     * The type collection.
     */
    private boolean typeCollection = false;

    /**
     * Instantiates a new cmis object.
     *
     * @param id the path
     * @param name the name
     */
    public CmisS3Object(String id, String name) {
        super();
        id = sanitize(id);
        this.id = id;
        this.name = name;
        this.typeCollection = id.endsWith("/");
    }

    /**
     * Sanitize.
     *
     * @param path the path
     * @return the string
     */
    @Override
    public String sanitize(String path) {
        return path.replace("\\", "")
                   .replaceAll("\\/\\/", "/");
    }

    /**
     * Returns the ID of this CmisS3Object.
     *
     * @return the Id
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Returns the Name of this CmisS3Object.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Returns the Type of this CmisS3Object.
     *
     * @return the object type
     */
    @Override
    public ObjectType getType() {
        return this.isCollection() ? ObjectType.FOLDER : ObjectType.DOCUMENT;
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    protected boolean isCollection() {
        return typeCollection;
    }

    /**
     * Delete this CmisS3Object.
     *
     * @param allVersions whether to delete all versions
     * @throws IOException IO Exception
     */
    @Override
    public void delete(boolean allVersions) throws IOException {
        delete();
    }

    /**
     * Delete this CmisS3Object.
     */
    @Override
    public void delete() {
        if (this.typeCollection) {
            S3Facade.deleteFolder(this.id);
        } else {
            S3Facade.delete(this.id);
        }
    }

    /**
     * Rename this CmisS3Object.
     *
     * @param newName the new name
     */
    @Override
    public void rename(String newName) {
        // TODO see how to rename from S3Facade
        // S3Facade.update();
    }

}
