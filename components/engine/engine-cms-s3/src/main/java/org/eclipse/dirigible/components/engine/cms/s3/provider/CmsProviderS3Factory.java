/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.provider;

import org.eclipse.dirigible.components.engine.cms.CmsProvider;
import org.eclipse.dirigible.components.engine.cms.CmsProviderFactory;
import org.eclipse.dirigible.components.engine.cms.CmsProviderInitializationException;
import org.eclipse.dirigible.components.engine.cms.s3.repository.CmisS3Session;
import org.springframework.stereotype.Component;

/**
 * A factory for creating CmsProviderS3 objects.
 */
@Component("cms-provider-s3")
class CmsProviderS3Factory implements CmsProviderFactory {

    /**
     * Creates the.
     *
     * @return the cms provider
     * @throws CmsProviderInitializationException the cms provider initialization exception
     */
    @Override
    public CmsProvider create() throws CmsProviderInitializationException {
        CmisS3Session session = new CmisS3Session();
        return new CmsProviderS3(session);
    }
}
