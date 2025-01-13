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
import org.eclipse.dirigible.components.engine.cms.s3.repository.CmisS3Session;

/**
 * The Class CmsProviderS3.
 */
public class CmsProviderS3 implements CmsProvider {

    /**
     * The Constant TYPE.
     */
    public static final String TYPE = "s3"; //$NON-NLS-1$
    private final CmisS3Session session;

    CmsProviderS3(CmisS3Session session) {

        this.session = session;
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    @Override
    public Object getSession() {
        return session;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    @Override
    public String getType() {
        return TYPE;
    }

}
