/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.master.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.repository.api.RepositoryInitializationException;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.local.LocalRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Zip Repository.
 */
public class ZipRepository extends FileSystemRepository {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(ZipRepository.class);

    /** The zip repository root folder. */
    private String zipRepositoryRootFolder;

    /**
     * Instantiates a new zip repository.
     *
     * @param zip the zip
     * @throws LocalRepositoryException the local repository exception
     */
    public ZipRepository(String zip) throws LocalRepositoryException {

        File zipFile = new File(zip);
        if (zipFile.exists()) {
            try {
                Path rootFolder = Files.createTempDirectory("zip_repository");
                String zipFileName = zipFile.getName();
                zipRepositoryRootFolder = zipFileName.substring(0, zipFileName.lastIndexOf("."));
                Path unpackFolder = Paths.get(rootFolder.toString(), zipRepositoryRootFolder, "root");
                unpackZip(new FileInputStream(zip), unpackFolder.toString());
                createRepository(rootFolder.toString(), true);
            } catch (IOException e) {
                throw new LocalRepositoryException(e);
            }
        } else {
            throw new LocalRepositoryException(String.format("Zip file containing Repository content does not exist at path: %s", zip));
        }
    }

    /**
     * Unpack zip.
     *
     * @param zip the zip
     * @param folder the folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void unpackZip(InputStream zip, String folder) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(zip);
        try {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                name = Paths.get(FilenameUtils.normalize(name))
                            .normalize()
                            .toString();
                File entryDestination = new File(folder, name);
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(entryDestination.getCanonicalFile());
                } else {
                    FileUtils.forceMkdir(entryDestination.getParentFile()
                                                         .getCanonicalFile());
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(entryDestination);
                        IOUtils.copy(zipInputStream, out);
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            }
        } finally {
            zipInputStream.close();
        }
    }

    /**
     * Instantiates a new zip repository.
     *
     * @param rootFolder the root folder
     * @param absolute the absolute
     * @throws LocalRepositoryException the local repository exception
     */
    // disable usage
    protected ZipRepository(String rootFolder, boolean absolute) throws LocalRepositoryException {
        super(rootFolder, absolute);
    }

    /**
     * Instantiates a new zip repository.
     *
     * @throws LocalRepositoryException the local repository exception
     */
    // disable usage
    protected ZipRepository() throws LocalRepositoryException {
        super();
    }

    /**
     * Gets the repository root folder.
     *
     * @return the repository root folder
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.fs.FileSystemRepository#getRepositoryRootFolder()
     */
    @Override
    protected String getRepositoryRootFolder() {
        return this.zipRepositoryRootFolder;
    }

    /**
     * Initialize.
     *
     * @throws RepositoryInitializationException the repository initialization exception
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.dirigible.repository.api.IRepositoryReader#initialize()
     */
    @Override
    public void initialize() throws RepositoryInitializationException {
        // TODO Auto-generated method stub

    }

    /**
     * Checks if is linking paths supported.
     *
     * @return true, if is linking paths supported
     */
    @Override
    public boolean isLinkingPathsSupported() {
        return false;
    }

    /**
     * Link path.
     *
     * @param repositoryPath the repository path
     * @param filePath the file path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void linkPath(String repositoryPath, String filePath) throws IOException {
        throw new UnsupportedOperationException("Linking of external paths not supported for this Repository type");
    }

    /**
     * Gets the last modified.
     *
     * @return the last modified
     */
    @Override
    public long getLastModified() {
        return 0;
    }

}
