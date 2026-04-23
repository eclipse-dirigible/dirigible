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

import org.eclipse.dirigible.components.engine.cms.CmisContentStream;

import java.io.InputStream;

public class CmisSharepointContentStream implements CmisContentStream {

    private final String filename;

    private final long length;

    private final String mimetype;

    private final InputStream inputStream;

    public CmisSharepointContentStream(String filename, long length, String mimetype, InputStream inputStream) {
        super();
        this.filename = filename;
        this.length = length;
        this.mimetype = mimetype;
        this.inputStream = inputStream;
    }

    @Override
    public InputStream getStream() {
        return this.inputStream;
    }

    /**
     * Gets the filename.
     *
     * @return the filename
     */
    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public String getMimeType() {
        return mimetype;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

}
