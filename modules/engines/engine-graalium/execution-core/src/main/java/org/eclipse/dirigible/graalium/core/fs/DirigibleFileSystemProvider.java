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
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 * The Class DirigibleFileSystemProvider.
 */
public class DirigibleFileSystemProvider extends FileSystemProvider {

    /** The internal file system provider. */
    private final FileSystemProvider internalFileSystemProvider;

    /**
     * Instantiates a new dirigible file system provider.
     *
     * @param internalFileSystemProvider the internal file system provider
     */
    private DirigibleFileSystemProvider(FileSystemProvider internalFileSystemProvider) {
        this.internalFileSystemProvider = internalFileSystemProvider;
    }

    /**
     * From another.
     *
     * @param fileSystemProvider the file system provider
     * @return the dirigible file system provider
     */
    public static DirigibleFileSystemProvider fromAnother(FileSystemProvider fileSystemProvider) {
        return new DirigibleFileSystemProvider(fileSystemProvider);
    }

    /**
     * Gets the scheme.
     *
     * @return the scheme
     */
    @Override
    public String getScheme() {
        return internalFileSystemProvider.getScheme();
    }

    /**
     * New file system.
     *
     * @param uri the uri
     * @param env the env
     * @return the file system
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        return DirigibleFileSystem.fromDefault();
    }

    /**
     * Gets the file system.
     *
     * @param uri the uri
     * @return the file system
     */
    @Override
    public FileSystem getFileSystem(URI uri) {
        return DirigibleFileSystem.fromDefault();
    }

    /**
     * Gets the path.
     *
     * @param uri the uri
     * @return the path
     */
    @Override
    public Path getPath(URI uri) {
        return DirigiblePath.fromAnother(internalFileSystemProvider.getPath(uri));
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
        return internalFileSystemProvider.newByteChannel(path, options, attrs);
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
        return internalFileSystemProvider.newDirectoryStream(dir, filter);
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
        internalFileSystemProvider.createDirectory(dir, attrs);
    }

    /**
     * Delete.
     *
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void delete(Path path) throws IOException {
        internalFileSystemProvider.delete(path);
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
        internalFileSystemProvider.copy(source, target, options);
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
        internalFileSystemProvider.move(source, target, options);
    }

    /**
     * Checks if is same file.
     *
     * @param path the path
     * @param path2 the path 2
     * @return true, if is same file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return internalFileSystemProvider.isSameFile(path, path2);
    }

    /**
     * Checks if is hidden.
     *
     * @param path the path
     * @return true, if is hidden
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public boolean isHidden(Path path) throws IOException {
        return internalFileSystemProvider.isHidden(path);
    }

    /**
     * Gets the file store.
     *
     * @param path the path
     * @return the file store
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return internalFileSystemProvider.getFileStore(path);
    }

    /**
     * Check access.
     *
     * @param path the path
     * @param modes the modes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        // System.out.println(123);
        internalFileSystemProvider.checkAccess(path, modes);
    }

    /**
     * Gets the file attribute view.
     *
     * @param <V> the value type
     * @param path the path
     * @param type the type
     * @param options the options
     * @return the file attribute view
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return internalFileSystemProvider.getFileAttributeView(path, type, options);
    }

    /**
     * Read attributes.
     *
     * @param <A> the generic type
     * @param path the path
     * @param type the type
     * @param options the options
     * @return the a
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return internalFileSystemProvider.readAttributes(path, type, options);
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
        return internalFileSystemProvider.readAttributes(path, attributes, options);
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
        internalFileSystemProvider.setAttribute(path, attribute, value, options);
    }


}
