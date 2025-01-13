/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import java.io.InputStream;

/**
 * The Interface CmisContentStream.
 */
public interface CmisContentStream {
    /**
     * Returns the InputStream of this ContentStream object.
     *
     * @return Input Stream
     */
    InputStream getStream();

    /**
     * Gets the filename.
     *
     * @return the filename
     */
    String getFilename();

    /**
     * Gets the length.
     *
     * @return the length
     */
    long getLength();

    /**
     * Gets the mime type.
     *
     * @return the mime type
     */
    String getMimeType();

    /**
     * Gets the input stream.
     *
     * @return the input stream
     */
    InputStream getInputStream();
}
