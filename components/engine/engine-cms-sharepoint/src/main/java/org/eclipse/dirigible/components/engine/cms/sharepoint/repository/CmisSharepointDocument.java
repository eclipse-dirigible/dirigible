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

import org.eclipse.dirigible.components.api.sharepoint.SharepointFacade;
import org.eclipse.dirigible.components.engine.cms.CmisContentStream;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;

import java.io.InputStream;

public class CmisSharepointDocument extends CmisSharepointObject implements CmisDocument {

    public CmisSharepointDocument(String id, String name) {
        super(id, name);
    }

    @Override
    protected boolean isCollection() {
        return false;
    }

    @Override
    public CmisContentStream getContentStream() {
        InputStream inputStream = SharepointFacade.getInputStream(getId());

        long contentLength = -1;
        String contentType = getContentType(getId());

        return new CmisSharepointContentStream(getName(), contentLength, contentType, inputStream);
    }

    private String getContentType(String resource) {
        return SharepointFacade.getObjectContentType(resource);
    }

    @Override
    public String getPath() {
        return getId();
    }

}
