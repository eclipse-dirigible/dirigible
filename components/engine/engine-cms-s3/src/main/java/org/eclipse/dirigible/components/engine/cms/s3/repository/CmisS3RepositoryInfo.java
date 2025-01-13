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

import org.eclipse.dirigible.components.engine.cms.CmisRepositoryInfo;

/**
 * The Class CmisS3RepositoryInfo.
 */
public class CmisS3RepositoryInfo implements CmisRepositoryInfo {

    /** The session. */
    private CmisS3Session session;

    /**
     * Instantiates a new repository info.
     *
     * @param session the session
     */
    public CmisS3RepositoryInfo(CmisS3Session session) {
        super();
        this.session = session;
    }

    /**
     * Returns the ID of the CMIS repository.
     *
     * @return the Id
     */
    // TODO FIX
    public String getId() {
        // return this.session.getCmisRepository()
        // .getInternalObject()
        // .getClass()
        // .getCanonicalName();
        return "";
    }

    /**
     * Returns the Name of the CMIS repository.
     *
     * @return the Name
     */
    // TODO FIX
    public String getName() {
        // return this.session.getCmisRepository()
        // .getInternalObject()
        // .getClass()
        // .getCanonicalName();
        return "";
    }

}
