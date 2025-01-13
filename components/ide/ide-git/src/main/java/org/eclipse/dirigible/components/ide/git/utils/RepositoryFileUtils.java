/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IEntity;
import org.eclipse.dirigible.repository.api.IResource;

/**
 * The file system to repository transformation utility.
 */
public class RepositoryFileUtils {

    /**
     * Creates the directory.
     *
     * @param directory the directory
     * @return the file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static File createDirectory(String directory) throws IOException {
        return FileSystemUtils.forceCreateDirectory(directory);
    }

    /**
     * Copy collection to directory.
     *
     * @param source the source
     * @param tempDirectory the temp directory
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyCollectionToDirectory(ICollection source, File tempDirectory) throws IOException {
        copyCollectionToDirectory(source, tempDirectory, new String[] {});
    }

    /**
     * Copy collection to directory.
     *
     * @param source the source
     * @param tempDirectory the temp directory
     * @param roots the roots
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyCollectionToDirectory(ICollection source, File tempDirectory, String... roots) throws IOException {
        if (!source.exists()) {
            return;
        }
        for (IEntity entity : source.getChildren()) {
            if (entity instanceof ICollection) {
                copyCollectionToDirectory((ICollection) entity, tempDirectory, roots);

            }
            if (entity instanceof IResource) {
                String path = entity.getParent()
                                    .getPath();
                StringBuilder resourceDirectory = new StringBuilder();
                resourceDirectory.append(path);
                resourceDirectory.append(File.separator);

                String directoryPath = resourceDirectory.toString();
                for (String root : roots) {
                    if (directoryPath.startsWith(root)) {
                        directoryPath = directoryPath.substring(root.length());
                        break;
                    }
                }
                File baseDirectory = new File(tempDirectory, directoryPath);
                FileUtils.forceMkdir(baseDirectory.getCanonicalFile());

                String resourcePath = entity.getPath();
                for (String root : roots) {
                    if (resourcePath.startsWith(root)) {
                        resourcePath = resourcePath.substring(root.length());
                        break;
                    }
                }

                try (InputStream in = new ByteArrayInputStream(((IResource) entity).getContent())) {
                    File outputFile = new File(tempDirectory, resourcePath);
                    try (FileOutputStream out = new FileOutputStream(outputFile)) {
                        IOUtils.copy(in, out);
                        out.flush();
                    }
                }
            }
        }
    }

}
