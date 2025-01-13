/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.eclipse.dirigible.repository.api.IRepository;

import java.util.Objects;

/**
 * The Class CmisS3Utils.
 */
public class CmisS3Utils {

    /** The root. */
    public static String ROOT = "/";

    /**
     * Find current file.
     *
     * @param folderPath the folder path
     * @return the string
     */
    public static String findCurrentFile(String folderPath) {
        if (Objects.equals(folderPath, ROOT)) {
            return ROOT;
        }

        String[] parts = folderPath.split(IRepository.SEPARATOR);
        return parts[parts.length - 1];
    }

    /**
     * Find current folder.
     *
     * @param folderPath the folder path
     * @return the string
     */
    public static String findCurrentFolder(String folderPath) {
        if (Objects.equals(folderPath, ROOT)) {
            return ROOT;
        }

        String[] parts = folderPath.split(IRepository.SEPARATOR);
        return parts[parts.length - 1] + IRepository.SEPARATOR;
    }

    /**
     * Find parent folder.
     *
     * @param folderPath the folder path
     * @return the string
     */
    public static String findParentFolder(String folderPath) {
        if (Objects.equals(folderPath, ROOT)) {
            return null;
        }

        String[] parts = folderPath.split(IRepository.SEPARATOR);
        if (parts.length >= 3) {
            int secondToLastIndex = parts.length - 2;
            return parts[secondToLastIndex] + IRepository.SEPARATOR;
        } else {
            return IRepository.SEPARATOR;
        }
    }

}
