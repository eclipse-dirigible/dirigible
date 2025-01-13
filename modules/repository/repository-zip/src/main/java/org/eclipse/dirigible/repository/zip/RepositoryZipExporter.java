/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.zip;

import static java.text.MessageFormat.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IEntity;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryExportException;
import org.eclipse.dirigible.repository.api.RepositoryReadException;

/**
 * Utility class which exports all the content under a given path.
 */
public class RepositoryZipExporter {

    /**
     * Export all the content under the given path(s) with the target repository instance Include the
     * last segment of the relative roots during the archiving.
     *
     * @param repository the target {@link IRepository} instance
     * @param relativeRoots the relative roots
     * @return the exported content
     * @throws RepositoryExportException in case the content cannot be exported
     */
    public static byte[] exportZip(IRepository repository, List<String> relativeRoots) throws RepositoryExportException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = null;
            try {
                zipOutputStream = new ZipOutputStream(baos);

                for (String relativeRoot : relativeRoots) {
                    ICollection collection = repository.getCollection(relativeRoot);
                    if (collection.exists()) {
                        traverseCollection(zipOutputStream, collection, relativeRoot.length() - collection.getName()
                                                                                                          .length());
                    } else {
                        IResource iResource = repository.getResource(relativeRoot);
                        if (iResource.exists()) {
                            ZipEntry zipEntry = new ZipEntry(iResource.getPath()
                                                                      .substring(relativeRoot.length() - iResource.getName()
                                                                                                                  .length()));
                            zipOutputStream.putNextEntry(zipEntry);
                            zipOutputStream.write((iResource.getContent() == null ? new byte[] {} : iResource.getContent()));
                            zipOutputStream.closeEntry();
                        } else {
                            throw new IOException(format("Relative Root: {0} doesn't exist", relativeRoot));
                        }
                    }
                }
            } finally {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
            }

            byte[] result = baos.toByteArray();
            return result;
        } catch (RepositoryReadException | IOException e) {
            throw new RepositoryExportException(e);
        }
    }

    /**
     * Export all the content under the given path with the target repository instance Include or NOT
     * the last segment of the relative root during the archiving.
     *
     * @param repository the repository
     * @param relativeRoot single root
     * @param inclusive whether to include the last segment of the root or to pack its content directly
     *        in the archive
     * @return the exported content
     * @throws RepositoryExportException in case the content cannot be exported
     */
    public static byte[] exportZip(IRepository repository, String relativeRoot, boolean inclusive) throws RepositoryExportException {

        List<String> relativeRoots = new ArrayList<String>();

        ICollection collection = repository.getCollection(relativeRoot);
        if (collection.exists()) {

            if (inclusive) {
                relativeRoots.add(relativeRoot);
            } else {
                List<IEntity> entities = collection.getChildren();
                for (IEntity iEntity : entities) {
                    relativeRoots.add(iEntity.getPath());
                }
            }
            return exportZip(repository, relativeRoots);
        }
        IResource resource = repository.getResource(relativeRoot);
        if (resource.exists()) {
            relativeRoots.add(resource.getPath());
            return exportZip(repository, relativeRoots);
        }
        throw new RepositoryExportException(format("Relative Root: {0} does not exist", relativeRoot));
    }

    /**
     * Iterate recursively a given collection and put its content to the zip.
     *
     * @param zipOutputStream resulting output stream
     * @param collection the {ICollection} to be processed
     * @param substring the prefix size
     * @throws RepositoryExportException in case the processing fails
     */
    private static void traverseCollection(ZipOutputStream zipOutputStream, ICollection collection, int substring)
            throws RepositoryExportException {
        try {
            ZipEntry zipEntry = null;
            if (collection.getPath()
                          .length() >= substring) {
                zipEntry = new ZipEntry(collection.getPath()
                                                  .substring(substring)
                        + IRepository.SEPARATOR);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.closeEntry();
            }

            List<ICollection> collections = collection.getCollections();
            for (ICollection iCollection : collections) {
                traverseCollection(zipOutputStream, iCollection, substring);
            }

            List<IResource> resources = collection.getResources();
            for (IResource iResource : resources) {
                zipEntry = new ZipEntry(iResource.getPath()
                                                 .substring(substring));
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write((iResource.getContent() == null ? new byte[] {} : iResource.getContent()));
                zipOutputStream.closeEntry();
            }
        } catch (RepositoryReadException | IOException e) {
            throw new RepositoryExportException(e);
        }
    }

}
