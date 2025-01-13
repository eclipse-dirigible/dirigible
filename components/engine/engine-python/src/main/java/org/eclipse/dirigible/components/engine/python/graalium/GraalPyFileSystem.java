/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.python.graalium;

import org.graalvm.polyglot.io.FileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

public class GraalPyFileSystem implements FileSystem {

    /**
     * The Constant DELEGATE.
     */
    private final FileSystemProvider delegateFileSystemProvider;

    /**
     * The current working directory path.
     */
    private Path currentWorkingDirectoryPath;

    /**
     * Instantiates a new graal JS file system.
     *
     * @param currentWorkingDirectoryPath the current working directory path
     * @param delegateFileSystem the file system to delegate to
     */
    public GraalPyFileSystem(Path currentWorkingDirectoryPath, java.nio.file.FileSystem delegateFileSystem) {
        this.currentWorkingDirectoryPath = currentWorkingDirectoryPath;
        this.delegateFileSystemProvider = delegateFileSystem.provider();
    }

    /**
     * Parses the path.
     *
     * @param uri the uri
     * @return the path
     */
    @Override
    public Path parsePath(URI uri) {
        throw new RuntimeException("Importing modules from URIs not supported for now.");
    }

    /**
     * Parses the path.
     *
     * @param path the path
     * @return the path
     */
    @Override
    public Path parsePath(String path) {
        if ("".equals(path)) {
            return currentWorkingDirectoryPath;
        }

        return Path.of(path);
    }

    /**
     * To absolute path.
     *
     * @param path the path
     * @return the path
     */
    @Override
    public Path toAbsolutePath(Path path) {
        return path.toAbsolutePath();
    }

    /**
     * To real path.
     *
     * @param path the path
     * @param linkOptions the link options
     * @return the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
        if (path.isAbsolute() && !path.startsWith(currentWorkingDirectoryPath)) {
            path = currentWorkingDirectoryPath.resolve(path.toString()
                                                           .substring(1));
        }
        return path.toRealPath(linkOptions);
    }

    /**
     * New byte channel.
     *
     * @param path the path
     * @param options the options
     * @param attrs the attrs
     * @return the seekable byte channel
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return delegateFileSystemProvider.newByteChannel(path, options, attrs);
    }

    /**
     * Check access.
     *
     * @param path the path
     * @param modes the modes
     * @param linkOptions the link options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        if (isFollowLinks(linkOptions)) {
            delegateFileSystemProvider.checkAccess(path, modes.toArray(new AccessMode[0]));
        } else if (modes.isEmpty()) {
            delegateFileSystemProvider.readAttributes(path, "isRegularFile", LinkOption.NOFOLLOW_LINKS);
        } else {
            throw new UnsupportedOperationException(
                    "CheckAccess for NIO Provider is unsupported with non empty AccessMode and NOFOLLOW_LINKS.");
        }
    }

    /**
     * Creates the directory.
     *
     * @param dir the dir
     * @param attrs the attrs
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        delegateFileSystemProvider.createDirectory(dir, attrs);
    }

    /**
     * Delete.
     *
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void delete(Path path) throws IOException {
        delegateFileSystemProvider.delete(path);
    }

    /**
     * Copy.
     *
     * @param source the source
     * @param target the target
     * @param options the options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        delegateFileSystemProvider.copy(source, target, options);
    }

    /**
     * Move.
     *
     * @param source the source
     * @param target the target
     * @param options the options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        delegateFileSystemProvider.move(source, target, options);
    }

    /**
     * New directory stream.
     *
     * @param dir the dir
     * @param filter the filter
     * @return the directory stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return delegateFileSystemProvider.newDirectoryStream(dir, filter);
    }

    /**
     * Creates the link.
     *
     * @param link the link
     * @param existing the existing
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void createLink(Path link, Path existing) throws IOException {
        delegateFileSystemProvider.createLink(link, existing);
    }

    /**
     * Creates the symbolic link.
     *
     * @param link the link
     * @param target the target
     * @param attrs the attrs
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        delegateFileSystemProvider.createSymbolicLink(link, target, attrs);
    }

    /**
     * Read symbolic link.
     *
     * @param link the link
     * @return the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return delegateFileSystemProvider.readSymbolicLink(link);
    }

    /**
     * Read attributes.
     *
     * @param path the path
     * @param attributes the attributes
     * @param options the options
     * @return the map
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return delegateFileSystemProvider.readAttributes(path, attributes, options);
    }

    /**
     * Sets the attribute.
     *
     * @param path the path
     * @param attribute the attribute
     * @param value the value
     * @param options the options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        delegateFileSystemProvider.setAttribute(path, attribute, value, options);
    }

    /**
     * Sets the current working directory.
     *
     * @param currentWorkingDirectoryPath the new current working directory
     */
    @Override
    public void setCurrentWorkingDirectory(Path currentWorkingDirectoryPath) {
        this.currentWorkingDirectoryPath = currentWorkingDirectoryPath;
    }

    /**
     * Checks if is follow links.
     *
     * @param linkOptions the link options
     * @return true, if is follow links
     */
    private static boolean isFollowLinks(final LinkOption... linkOptions) {
        for (LinkOption lo : linkOptions) {
            if (lo == LinkOption.NOFOLLOW_LINKS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if is same file.
     *
     * @param path1 the path 1
     * @param path2 the path 2
     * @param options the options
     * @return true, if is same file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public boolean isSameFile(Path path1, Path path2, LinkOption... options) throws IOException {
        return delegateFileSystemProvider.isSameFile(path1, path2);
    }

    /**
     * Gets the temp directory.
     *
     * @return the temp directory
     */
    @Override
    public Path getTempDirectory() {
        return new File(System.getProperty("java.io.tmpdir")).toPath();
    }
}
