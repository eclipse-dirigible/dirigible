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
 * Repository cache.
 */
public interface IRepositoryCache {

    /**
     * Gets file content from the repository cache by path.
     *
     * @param path the repository path
     * @return the file content
     */
    public byte[] get(String path);

    /**
     * Adds file content to the repository cache.
     *
     * @param path the repository path
     * @param content the file content
     */
    public void put(String path, byte[] content);

    /**
     * Remove file content from the repository cache by path.
     *
     * @param path the repository path
     */
    public void remove(String path);

    /**
     * Clear the repository cache.
     */
    public void clear();

}
