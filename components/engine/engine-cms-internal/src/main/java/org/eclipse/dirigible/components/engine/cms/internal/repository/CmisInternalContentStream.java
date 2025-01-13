/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import org.eclipse.dirigible.components.engine.cms.CmisContentStream;

import java.io.InputStream;

/**
 * The Class CmisInternalContentStream.
 */
public class CmisInternalContentStream implements CmisContentStream {

    /** The filename. */
    private final String filename;

    /** The length. */
    private final long length;

    /** The mimetype. */
    private final String mimetype;

    /** The input stream. */
    private final InputStream inputStream;

    /**
     * Instantiates a new content stream.
     *
     * @param filename the filename
     * @param length the length
     * @param mimetype the mimetype
     * @param inputStream the input stream
     */
    public CmisInternalContentStream(String filename, long length, String mimetype, InputStream inputStream) {
        super();
        this.filename = filename;
        this.length = length;
        this.mimetype = mimetype;
        this.inputStream = inputStream;
    }

    /**
     * Returns the InputStream of this CmisInternalContentStream object.
     *
     * @return Input Stream
     */
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

    /**
     * Gets the length.
     *
     * @return the length
     */
    @Override
    public long getLength() {
        return length;
    }

    /**
     * Gets the mime type.
     *
     * @return the mime type
     */
    @Override
    public String getMimeType() {
        return mimetype;
    }

    /**
     * Gets the input stream.
     *
     * @return the input stream
     */
    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

}
