/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.eclipse.dirigible.components.engine.cms.CmisObjectFactory;

import java.io.InputStream;

/**
 * A factory for creating Object objects.
 */
public class CmisS3ObjectFactory implements CmisObjectFactory {

    /**
     * Creates a new S3Object object.
     *
     * @param filename the filename
     * @param length the length
     * @param mimetype the mimetype
     * @param inputStream the input stream
     * @return the content stream
     */
    @Override
    public CmisS3ContentStream createContentStream(String filename, long length, String mimetype, InputStream inputStream) {
        return new CmisS3ContentStream(filename, length, mimetype, inputStream);
    }

}
