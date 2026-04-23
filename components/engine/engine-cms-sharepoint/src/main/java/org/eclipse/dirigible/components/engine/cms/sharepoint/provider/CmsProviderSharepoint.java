/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.sharepoint.provider;

import org.eclipse.dirigible.components.engine.cms.CmsProvider;
import org.eclipse.dirigible.components.engine.cms.sharepoint.repository.CmisSharepointSession;

public class CmsProviderSharepoint implements CmsProvider {

    private static final String TYPE = "ms-sharepoint";

    private final CmisSharepointSession session;

    CmsProviderSharepoint(CmisSharepointSession session) {

        this.session = session;
    }

    @Override
    public Object getSession() {
        return session;
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
