/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

import java.util.Date;

/**
 * The interface representing a version instance of an {@link IResource}.
 */
public interface IResourceVersion extends Comparable<IResourceVersion> {

    /**
     * Returns the path of this resource version
     * <p>
     * The result may not be <code>null</code>.
     * <p>
     * Example: /db/users/test.txt <br>
     * Example: /db/articles
     *
     * @return the location of the {@link IResource}
     */
    public String getPath();

    /**
     * Returns the version number.
     *
     * @return the version
     */
    public int getVersion();

    /**
     * Returns the content of the resource version as a byte array.
     *
     * @return the raw content
     * @throws RepositoryReadException the repository read exception
     */
    public byte[] getContent() throws RepositoryReadException;

    /**
     * Getter for binary flag.
     *
     * @return whether it is binary
     */
    public boolean isBinary();

    /**
     * Getter for the content type.
     *
     * @return the type of the content
     */
    public String getContentType();

    /**
     * The creator of the entity.
     *
     * @return the user created this version
     */
    public String getCreatedBy();

    /**
     * Timestamp of the creation of the entity.
     *
     * @return the time this version has been created
     */
    public Date getCreatedAt();

}
