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

/**
 * The <code>IResource</code> interface represents a resource located in the repository.
 */
public interface IResource extends IEntity {

    /** The default content type - text/plain. */
    public static final String CONTENT_TYPE_DEFAULT = "text/plain"; //$NON-NLS-1$

    /**
     * Returns the content of the resource as a byte array.
     *
     * @return the raw content
     * @throws RepositoryReadException in case the content cannot be retrieved
     */
    public byte[] getContent() throws RepositoryReadException;

    /**
     * Sets this resource's content.
     *
     * @param content the raw content
     * @throws RepositoryWriteException the repository write exception
     */
    public void setContent(byte[] content) throws RepositoryWriteException;

    /**
     * Sets this resource's content.
     *
     * @param content the raw content
     * @param isBinary whether it is binary
     * @param contentType the type of the content
     * @throws RepositoryWriteException in case the content of the {@link IResource} cannot be retrieved
     */
    public void setContent(byte[] content, boolean isBinary, String contentType) throws RepositoryWriteException;

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

}
