/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal.repository;

import org.eclipse.dirigible.components.engine.cms.CmisObjectFactory;

import java.io.InputStream;

/**
 * A factory for creating Object objects.
 */
public class CmisInternalObjectFactory implements CmisObjectFactory {

    /** The session. */
    private final CmisInternalSession session;

    /**
     * Instantiates a new object factory.
     *
     * @param session the session
     */
    public CmisInternalObjectFactory(CmisInternalSession session) {
        super();
        this.session = session;
    }

    /**
     * Creates a new Object object.
     *
     * @param filename the filename
     * @param length the length
     * @param mimetype the mimetype
     * @param inputStream the input stream
     * @return the content stream
     */
    @Override
    public CmisInternalContentStream createContentStream(String filename, long length, String mimetype, InputStream inputStream) {
        return new CmisInternalContentStream(filename, length, mimetype, inputStream);
    }

}
