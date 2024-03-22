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
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import java.io.InputStream;

/**
 * A factory for creating Object objects.
 */
public class CmisS3ObjectFactory {

    /**
     * Creates a new S3Object object.
     *
     * @param filename the filename
     * @param length the length
     * @param mimetype the mimetype
     * @param inputStream the input stream
     * @return the content stream
     */
    public CmisS3ContentStream createContentStream(String filename, long length, String mimetype, InputStream inputStream) {
        return new CmisS3ContentStream(filename, length, mimetype, inputStream);
    }

}
