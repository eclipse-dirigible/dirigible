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
import org.eclipse.dirigible.components.engine.cms.CmsProviderFactory;
import org.eclipse.dirigible.components.engine.cms.CmsProviderInitializationException;
import org.eclipse.dirigible.components.engine.cms.sharepoint.repository.CmisSharepointSession;
import org.springframework.stereotype.Component;

@Component("cms-provider-ms-sharepoint")
class CmsProviderSharepointFactory implements CmsProviderFactory {

    /**
     * Creates the.
     *
     * @return the cms provider
     * @throws CmsProviderInitializationException the cms provider initialization exception
     */
    @Override
    public CmsProvider create() throws CmsProviderInitializationException {
        CmisSharepointSession session = new CmisSharepointSession();
        return new CmsProviderSharepoint(session);
    }
}
