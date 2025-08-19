/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import java.io.IOException;

/**
 * The Interface CmisSession.
 */
public interface CmisSession {
    /**
     * Returns a CMIS Object by name.
     *
     * @param id the Id
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    CmisObject getObject(String id) throws IOException;

    /**
     * Returns the information about the CMIS repository.
     *
     * @return Repository Info
     */
    CmisRepositoryInfo getRepositoryInfo();

    /**
     * Returns the root folder of this repository.
     *
     * @return CmisFolder
     * @throws IOException IO Exception
     */
    CmisFolder getRootFolder() throws IOException;

    /**
     * Returns the ObjectFactory utility.
     *
     * @return Object Factory
     */
    CmisObjectFactory getObjectFactory();

    /**
     * Returns a CMIS Object by path.
     *
     * @param path the path
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    CmisObject getObjectByPath(String path) throws IOException;
}
