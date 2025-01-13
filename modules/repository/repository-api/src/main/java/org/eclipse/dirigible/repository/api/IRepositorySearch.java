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

import java.util.List;

/**
 * The interface containing the search related methods of the repository .
 */
public interface IRepositorySearch {

    /**
     * Search the given parameter in the names of the files and folders ( means *parameter).
     *
     * @param parameter the search text
     * @param caseInsensitive whether to be case insensitive
     * @return a list of {@link IEntity} instances
     * @throws RepositorySearchException in case the search fails
     */
    public List<IEntity> searchName(String parameter, boolean caseInsensitive) throws RepositorySearchException;

    /**
     * Search the given parameter in the names of the files and folders ( means *parameter) under
     * specified root folder (means *root).
     *
     * @param root the root location to start the search from
     * @param parameter the search text
     * @param caseInsensitive whether to be case insensitive
     * @return a list of {@link IEntity} instances
     * @throws RepositorySearchException in case the search fails
     */
    public List<IEntity> searchName(String root, String parameter, boolean caseInsensitive) throws RepositorySearchException;

    /**
     * Search the given given parameter in the names of the files and folders (means *parameter*).
     *
     * @param parameter the search text
     * @param caseInsensitive whether to be case insensitive
     * @return a list of {@link IEntity} instances
     * @throws RepositorySearchException in case the search fails
     */
    public List<IEntity> searchPath(String parameter, boolean caseInsensitive) throws RepositorySearchException;

    /**
     * Search the given given parameter in the names of the files and folders as well as in the content
     * of the text files.
     *
     * @param term the search text
     * @return a list of {@link IEntity} instances
     * @throws RepositorySearchException in case the search fails
     */
    public List<IEntity> searchText(String term) throws RepositorySearchException;

    /**
     * Re-index the content.
     *
     * @throws RepositorySearchException the repository search exception
     */
    public void searchRefresh() throws RepositorySearchException;

    /**
     * Find files by a given pattern.
     *
     * @param path the starting path
     * @param pattern the search pattern
     * @return a list of {@link IEntity} instances
     * @throws RepositorySearchException in case the search fails
     */
    public List<String> find(String path, String pattern) throws RepositorySearchException;

}
