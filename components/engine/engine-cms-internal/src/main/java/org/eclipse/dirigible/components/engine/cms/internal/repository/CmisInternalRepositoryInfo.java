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

import org.eclipse.dirigible.components.engine.cms.CmisRepositoryInfo;

/**
 * The Class CmisInternalRepositoryInfo.
 */
public class CmisInternalRepositoryInfo implements CmisRepositoryInfo {

    /** The session. */
    private CmisInternalSession session;

    /**
     * Instantiates a new repository info.
     *
     * @param session the session
     */
    public CmisInternalRepositoryInfo(CmisInternalSession session) {
        super();
        this.session = session;
    }

    /**
     * Returns the ID of the CMIS repository.
     *
     * @return the Id
     */
    @Override
    public String getId() {
        return this.session.getCmisRepository()
                           .getInternalObject()
                           .getClass()
                           .getCanonicalName();
    }

    /**
     * Returns the Name of the CMIS repository.
     *
     * @return the Name
     */
    @Override
    public String getName() {
        return this.session.getCmisRepository()
                           .getInternalObject()
                           .getClass()
                           .getCanonicalName();
    }

}
