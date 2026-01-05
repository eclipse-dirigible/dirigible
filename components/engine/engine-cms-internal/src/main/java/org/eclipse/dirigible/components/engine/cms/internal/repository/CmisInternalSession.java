/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;

import java.io.IOException;

/**
 * The Class CmisInternalSession.
 */
public class CmisInternalSession implements CmisSession {

    /** The cmis repository. */
    private final CmisRepository cmisRepository;

    /**
     * Instantiates a new cmis session.
     *
     * @param cmisRepository the cmis repository
     */
    public CmisInternalSession(CmisRepository cmisRepository) {
        super();
        this.cmisRepository = cmisRepository;
    }

    /**
     * Gets the cmis repository.
     *
     * @return the cmis repository
     */
    public CmisRepository getCmisRepository() {
        return cmisRepository;
    }

    /**
     * Returns the information about the CMIS repository.
     *
     * @return Repository Info
     */
    @Override
    public CmisInternalRepositoryInfo getRepositoryInfo() {
        return new CmisInternalRepositoryInfo(this);
    }

    /**
     * Returns the ObjectFactory utility.
     *
     * @return Object Factory
     */
    @Override
    public CmisInternalObjectFactory getObjectFactory() {
        return new CmisInternalObjectFactory(this);
    }

    /**
     * Returns the root folder of this repository.
     *
     * @return CmisInternalFolder
     * @throws IOException IO Exception
     */
    @Override
    public CmisInternalFolder getRootFolder() throws IOException {
        return new CmisInternalFolder(this);
    }

    /**
     * Returns a CMIS Object by path.
     *
     * @param path the path
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    @Override
    public CmisObject getObjectByPath(String path) throws IOException {
        return getObject(path);
    }

    /**
     * Returns a CMIS Object by name.
     *
     * @param id the Id
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    @Override
    public CmisObject getObject(String id) throws IOException {
        CmisInternalObject cmisObject = new CmisInternalObject(this, id);
        if (null == cmisObject.getInternalEntity() || !cmisObject.getInternalEntity()
                                                                 .exists()) {
            throw new IOException(String.format("Object with id: %s does not exist", id));
        }
        if (CmisConstants.OBJECT_TYPE_FOLDER.equals(cmisObject.getType()
                                                              .getId())) {
            return new CmisInternalFolder(this, id);
        } else if (CmisConstants.OBJECT_TYPE_DOCUMENT.equals(cmisObject.getType()
                                                                       .getId())) {
            return new CmisInternalDocument(this, id);
        }
        return cmisObject;
    }

}
