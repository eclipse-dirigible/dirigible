/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.eclipse.dirigible.components.api.s3.S3Facade;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.repository.api.IRepository;

import java.io.IOException;

/**
 * The Class CmisS3Session.
 */
public class CmisS3Session implements CmisSession {

    private static final String ROOT = "/";

    /**
     * Returns the information about the CMIS repository.
     *
     * @return Repository Info
     */
    public CmisS3RepositoryInfo getRepositoryInfo() {
        return new CmisS3RepositoryInfo(this);
    }

    /**
     * Returns the CmisS3ObjectFactory utility.
     *
     * @return Object Factory
     */
    public CmisS3ObjectFactory getObjectFactory() {
        return new CmisS3ObjectFactory();
    }

    /**
     * Returns the root folder of this repository.
     *
     * @return CmisS3Folder
     */

    public CmisS3Folder getRootFolder() {
        return new CmisS3Folder(ROOT, ROOT, true);
    }

    /**
     * Returns a CMIS Object by path.
     *
     * @param path the path
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    @Override
    public CmisS3Object getObjectByPath(String path) throws IOException {
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
    public CmisS3Object getObject(String id) throws IOException {
        if (isFolder(id)) {
            return new CmisS3Folder(id, CmisS3Utils.findCurrentFolder(id), ROOT.equals(id));
        }
        if (S3Facade.exists(id)) {
            return new CmisS3Document(id, CmisS3Utils.findCurrentFile(id));
        } else {
            throw new IOException("Missing object with id [" + id + "]");
        }
    }

    private boolean isFolder(String path) {
        return path.endsWith(IRepository.SEPARATOR);
    }
}
