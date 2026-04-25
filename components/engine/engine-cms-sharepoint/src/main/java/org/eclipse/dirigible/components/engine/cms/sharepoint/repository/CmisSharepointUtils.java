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

import com.microsoft.graph.models.DriveItem;
import org.eclipse.dirigible.repository.api.IRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class CmisSharepointUtils {

    private static final String ROOT = "/";

    static Set<SharepointObjectDescriptor> getDirectChildren(String rootPath, List<DriveItem> driveItems) {
        return getDirectChildren(rootPath, new HashSet<>(driveItems));
    }

    static Set<SharepointObjectDescriptor> getDirectChildren(String rootPath, Set<DriveItem> driveItems) {
        Set<SharepointObjectDescriptor> descriptors = new HashSet<>();

        for (DriveItem driveItem : driveItems) {
            String objectKey = driveItem.getName();
            boolean folder = driveItem.getFolder() != null;
            if (Objects.equals(rootPath, objectKey)) {
                continue;
            }
            descriptors.add(new SharepointObjectDescriptor(folder, objectKey));
        }

        return descriptors;
    }

    public static String findCurrentFile(String folderPath) {
        if (Objects.equals(folderPath, ROOT)) {
            return ROOT;
        }

        String[] parts = folderPath.split(IRepository.SEPARATOR);
        return parts[parts.length - 1];
    }

    static String removeDoubleSlash(String path) {
        // Regex "/{2,}" matches 2 or more forward slashes
        return path.replaceAll(IRepository.SEPARATOR + "{2,}", IRepository.SEPARATOR);
    }

    public static String findCurrentFolder(String folderPath) {
        if (Objects.equals(folderPath, ROOT)) {
            return ROOT;
        }

        String[] parts = folderPath.split(IRepository.SEPARATOR);
        return parts[parts.length - 1] + IRepository.SEPARATOR;
    }

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
