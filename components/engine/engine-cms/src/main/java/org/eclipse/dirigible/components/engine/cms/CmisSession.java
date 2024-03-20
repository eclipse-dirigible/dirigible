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
    public CmisObject getObject(String id) throws IOException;

    /**
     * Returns a CMIS Object by path.
     *
     * @param path the path
     * @return CMIS Object
     * @throws IOException IO Exception
     */
    public CmisObject getObjectByPath(String path) throws IOException;
}
