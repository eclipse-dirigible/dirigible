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

import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * The interface with the import methods for the repository .
 */
public interface IRepositoryImporter {

    /**
     * Imports content from zip file to the repository, based on the relative root.
     *
     * @param zipInputStream the input stream
     * @param relativeRoot the relative root
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(ZipInputStream zipInputStream, String relativeRoot) throws RepositoryImportException;

    /**
     * Imports content from zip file to the repository, based on the relative root. Overrides the
     * previous content depending on the override parameter.
     *
     * @param zipInputStream the input stream
     * @param relativeRoot the relative root
     * @param override whether to override existing
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(ZipInputStream zipInputStream, String relativeRoot, boolean override) throws RepositoryImportException;

    /**
     * Imports content from zip file to the repository, based on the relative root. Overrides the
     * previous content depending on the override parameter. Excludes the name of the root folder,
     * during the import, based on the excludeRootFolderName parameter.
     *
     * @param zipInputStream the input stream
     * @param relativeRoot the relative root
     * @param override whether to override existing
     * @param excludeRootFolderName whether to exclude the root folder name
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(ZipInputStream zipInputStream, String relativeRoot, boolean override, boolean excludeRootFolderName)
            throws RepositoryImportException;

    /**
     * Imports content from zip file to the repository, based on the relative root.
     *
     * @param data the zip file as byte array
     * @param relativeRoot the relative root
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(byte[] data, String relativeRoot) throws RepositoryImportException;

    /**
     * Imports content from zip file to the repository, based on the relative root. Overrides the
     * previous content depending on the override parameter.
     *
     * @param data the zip file as byte array
     * @param relativeRoot the relative root
     * @param override whether to override existing
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(byte[] data, String relativeRoot, boolean override) throws RepositoryImportException;

    /**
     * Imports content from zip file to the repository, based on the relative root. Overrides the
     * previous content depending on the override parameter. Excludes the name of the root folder,
     * during the import, based on the excludeRootFolderName parameter.
     *
     * @param data the zip file as byte array
     * @param relativeRoot the relative root
     * @param override whether to override existing
     * @param excludeRootFolderName the exclude root folder name
     * @param filter a filter
     * @throws RepositoryImportException in case the zip cannot be imported
     */
    public void importZip(byte[] data, String relativeRoot, boolean override, boolean excludeRootFolderName, Map<String, String> filter)
            throws RepositoryImportException;

}
