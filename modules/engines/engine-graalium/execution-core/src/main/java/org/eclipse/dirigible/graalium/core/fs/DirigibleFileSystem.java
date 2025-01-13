/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.graalium.core.fs;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

/**
 * The Class DirigibleFileSystem.
 */
public class DirigibleFileSystem extends FileSystem {

    /** The Constant FROM_DEFAULT. */
    private static final DirigibleFileSystem FROM_DEFAULT = new DirigibleFileSystem(FileSystems.getDefault());

    /** The internal file system. */
    private final FileSystem internalFileSystem;

    /**
     * Instantiates a new dirigible file system.
     *
     * @param internalFileSystem the internal file system
     */
    private DirigibleFileSystem(FileSystem internalFileSystem) {
        this.internalFileSystem = internalFileSystem;
    }

    /**
     * From another.
     *
     * @param fileSystem the file system
     * @return the dirigible file system
     */
    public static DirigibleFileSystem fromAnother(FileSystem fileSystem) {
        return new DirigibleFileSystem(fileSystem);
    }

    /**
     * From default.
     *
     * @return the dirigible file system
     */
    public static DirigibleFileSystem fromDefault() {
        return FROM_DEFAULT;
    }

    /**
     * Provider.
     *
     * @return the file system provider
     */
    @Override
    public FileSystemProvider provider() {
        return internalFileSystem.provider();
    }

    /**
     * Close.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        internalFileSystem.close();
    }

    /**
     * Checks if is open.
     *
     * @return true, if is open
     */
    @Override
    public boolean isOpen() {
        return internalFileSystem.isOpen();
    }

    /**
     * Checks if is read only.
     *
     * @return true, if is read only
     */
    @Override
    public boolean isReadOnly() {
        return internalFileSystem.isReadOnly();
    }

    /**
     * Gets the separator.
     *
     * @return the separator
     */
    @Override
    public String getSeparator() {
        return internalFileSystem.getSeparator();
    }

    /**
     * Gets the root directories.
     *
     * @return the root directories
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return internalFileSystem.getRootDirectories();
    }

    /**
     * Gets the file stores.
     *
     * @return the file stores
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return internalFileSystem.getFileStores();
    }

    /**
     * Supported file attribute views.
     *
     * @return the sets the
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        return internalFileSystem.supportedFileAttributeViews();
    }

    /**
     * Gets the path.
     *
     * @param first the first
     * @param more the more
     * @return the path
     */
    @Override
    public Path getPath(String first, String... more) {
        return DirigiblePath.fromAnother(internalFileSystem.getPath(first, more));
    }

    /**
     * Gets the path matcher.
     *
     * @param syntaxAndPattern the syntax and pattern
     * @return the path matcher
     */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return internalFileSystem.getPathMatcher(syntaxAndPattern);
    }

    /**
     * Gets the user principal lookup service.
     *
     * @return the user principal lookup service
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return internalFileSystem.getUserPrincipalLookupService();
    }

    /**
     * New watch service.
     *
     * @return the watch service
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public WatchService newWatchService() throws IOException {
        return internalFileSystem.newWatchService();
    }
}
