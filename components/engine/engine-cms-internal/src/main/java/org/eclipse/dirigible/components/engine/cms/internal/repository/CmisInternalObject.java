/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import java.io.IOException;

import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.eclipse.dirigible.repository.api.IEntity;
import org.eclipse.dirigible.repository.api.IRepository;

/**
 * The Class CmisInternalObject.
 */
public class CmisInternalObject implements CmisObject {

    /** The session. */
    private CmisInternalSession session;

    /** The internal entity. */
    private IEntity internalEntity;

    /** The type collection. */
    private boolean typeCollection = false;

    /**
     * Instantiates a new cmis object.
     *
     * @param session the session
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public CmisInternalObject(CmisInternalSession session, String path) throws IOException {
        super();
        this.session = session;
        path = sanitize(path);
        IRepository repository = ((IRepository) session.getCmisRepository()
                                                       .getInternalObject());
        if (repository.hasCollection(path)) {
            this.internalEntity = repository.getCollection(path);
            this.typeCollection = true;
        } else if (repository.hasResource(path)) {
            this.internalEntity = repository.getResource(path);
            this.typeCollection = false;
        }
    }


    /**
     * Gets the internal entity.
     *
     * @return the internal entity
     */
    public IEntity getInternalEntity() {
        return internalEntity;
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
     * Sanitize.
     *
     * @param path the path
     * @return the string
     */
    @Override
    public String sanitize(String path) {
        return path.replace("\\", "");
    }

    /**
     * Returns the ID of this CmisInternalObject.
     *
     * @return the Id
     */
    @Override
    public String getId() {
        return this.getInternalEntity()
                   .getPath();
    }

    /**
     * Returns the Name of this CmisInternalObject.
     *
     * @return the name
     */
    @Override
    public String getName() {
        if ("".equals(this.getInternalEntity()
                          .getName())) {
            return "root";
        }
        return this.getInternalEntity()
                   .getName();
    }

    /**
     * Returns the Type of this CmisInternalObject.
     *
     * @return the object type
     */
    @Override
    public ObjectType getType() {
        return this.isCollection() ? ObjectType.FOLDER : ObjectType.DOCUMENT;
    }

    /**
     * Delete this CmisInternalObject.
     *
     * @throws IOException IO Exception
     */
    @Override
    public void delete() throws IOException {
        this.getInternalEntity()
            .delete();
    }

    /**
     * Delete this CmisInternalObject.
     *
     * @param allVersions whether to delete all versions
     * @throws IOException IO Exception
     */
    @Override
    public void delete(boolean allVersions) throws IOException {
        delete();
    }

    /**
     * Rename this CmisInternalObject.
     *
     * @param newName the new name
     * @throws IOException IO Exception
     */
    @Override
    public void rename(String newName) throws IOException {
        this.getInternalEntity()
            .renameTo(newName);
    }

}
