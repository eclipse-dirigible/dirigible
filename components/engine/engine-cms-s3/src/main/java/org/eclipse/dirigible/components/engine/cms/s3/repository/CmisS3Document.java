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

import org.eclipse.dirigible.components.api.s3.S3Facade;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * The Class CmisDocument.
 */
public class CmisS3Document extends CmisS3Object implements CmisDocument {

    /**
     * Instantiates a new document.
     *
     * @param id the idx
     * @param name the name
     */
    public CmisS3Document(String id, String name) {
        super(id, name);
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    @Override
    protected boolean isCollection() {
        return false;
    }

    /**
     * Returns the CmisS3ContentStream representing the contents of this CmisDocument.
     *
     * @return Content Stream
     * @throws IOException IO Exception
     */
    public CmisS3ContentStream getContentStream() throws IOException {
        byte[] content = S3Facade.get(getId());
        String contentType = getContentType(getId());
        return new CmisS3ContentStream(getName(), content.length, contentType, new ByteArrayInputStream(content));
    }

    /**
     * Gets the content type.
     *
     * @param resource the resource
     * @return the content type
     */
    private String getContentType(String resource) {
        return S3Facade.getObjectContentType(resource);
    }

    /**
     * Returns the Path of this CmisDocument.
     *
     * @return the path
     */
    public String getPath() {
        return getId();
    }

    /**
     * Gets the resource name.
     *
     * @param resource the resource
     * @return the resource name
     */
    private String getResourceName(String resource) {
        return getName();
    }
}
