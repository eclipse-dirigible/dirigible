/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.sharepoint.repository;

import org.eclipse.dirigible.components.engine.cms.CmisRepositoryInfo;

class CmisSharepointRepositoryInfo implements CmisRepositoryInfo {

    private final CmisSharepointSession session;

    public CmisSharepointRepositoryInfo(CmisSharepointSession session) {
        super();
        this.session = session;
    }

    public String getId() {
        // TODO: Do we need this at all?
        // returning some value
        return "ms-sharepoint-repo-id";
    }

    public String getName() {
        // TODO: Do we need this at all?
        // returning some value
        return "ms-sharepoint-repo-name";
    }

}
