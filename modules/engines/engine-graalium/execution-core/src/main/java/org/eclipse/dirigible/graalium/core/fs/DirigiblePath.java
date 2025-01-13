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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * The Class DirigiblePath.
 */
public class DirigiblePath implements Path {

    /** The internal path. */
    private final Path internalPath;

    /**
     * Instantiates a new dirigible path.
     *
     * @param internalPath the internal path
     */
    public DirigiblePath(Path internalPath) {
        this.internalPath = internalPath;
    }

    /**
     * From another.
     *
     * @param path the path
     * @return the dirigible path
     */
    public static DirigiblePath fromAnother(Path path) {
        return new DirigiblePath(path);
    }

    /**
     * Gets the file system.
     *
     * @return the file system
     */
    @Override
    public FileSystem getFileSystem() {
        return DirigibleFileSystem.fromDefault();
    }

    /**
     * Checks if is absolute.
     *
     * @return true, if is absolute
     */
    @Override
    public boolean isAbsolute() {
        return internalPath.isAbsolute();
    }

    /**
     * Gets the root.
     *
     * @return the root
     */
    @Override
    public Path getRoot() {
        return fromAnother(internalPath.getRoot());
    }

    /**
     * Gets the file name.
     *
     * @return the file name
     */
    @Override
    public Path getFileName() {
        return fromAnother(internalPath.getFileName());
    }

    /**
     * Gets the parent.
     *
     * @return the parent
     */
    @Override
    public Path getParent() {
        return fromAnother(internalPath.getParent());
    }

    /**
     * Gets the name count.
     *
     * @return the name count
     */
    @Override
    public int getNameCount() {
        return internalPath.getNameCount();
    }

    /**
     * Gets the name.
     *
     * @param index the index
     * @return the name
     */
    @Override
    public Path getName(int index) {
        return fromAnother(internalPath.getName(index));
    }

    /**
     * Subpath.
     *
     * @param beginIndex the begin index
     * @param endIndex the end index
     * @return the path
     */
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return fromAnother(internalPath.subpath(beginIndex, endIndex));
    }

    /**
     * Starts with.
     *
     * @param other the other
     * @return true, if successful
     */
    @Override
    public boolean startsWith(Path other) {
        return internalPath.startsWith(other);
    }

    /**
     * Starts with.
     *
     * @param other the other
     * @return true, if successful
     */
    @Override
    public boolean startsWith(String other) {
        return internalPath.startsWith(other);
    }

    /**
     * Ends with.
     *
     * @param other the other
     * @return true, if successful
     */
    @Override
    public boolean endsWith(Path other) {
        return internalPath.endsWith(other);
    }

    /**
     * Ends with.
     *
     * @param other the other
     * @return true, if successful
     */
    @Override
    public boolean endsWith(String other) {
        return internalPath.endsWith(other);
    }

    /**
     * Normalize.
     *
     * @return the path
     */
    @Override
    public Path normalize() {
        return fromAnother(internalPath.normalize());
    }

    /**
     * Resolve.
     *
     * @param other the other
     * @return the path
     */
    @Override
    public Path resolve(Path other) {
        return fromAnother(internalPath.resolve(other));
    }

    /**
     * Resolve.
     *
     * @param other the other
     * @return the path
     */
    @Override
    public Path resolve(String other) {
        return fromAnother(internalPath.resolve(other));
    }

    /**
     * Resolve sibling.
     *
     * @param other the other
     * @return the path
     */
    @Override
    public Path resolveSibling(Path other) {
        return fromAnother(internalPath.resolveSibling(other));
    }

    /**
     * Resolve sibling.
     *
     * @param other the other
     * @return the path
     */
    @Override
    public Path resolveSibling(String other) {
        return fromAnother(internalPath.resolveSibling(other));
    }

    /**
     * Relativize.
     *
     * @param other the other
     * @return the path
     */
    @Override
    public Path relativize(Path other) {
        return fromAnother(internalPath.relativize(other));
    }

    /**
     * To uri.
     *
     * @return the uri
     */
    @Override
    public URI toUri() {
        return internalPath.toUri();
    }

    /**
     * To absolute path.
     *
     * @return the path
     */
    @Override
    public Path toAbsolutePath() {
        return fromAnother(internalPath.toAbsolutePath());
    }

    /**
     * To real path.
     *
     * @param options the options
     * @return the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return fromAnother(internalPath.toRealPath(options));
    }

    /**
     * To file.
     *
     * @return the file
     */
    @Override
    public File toFile() {
        return internalPath.toFile();
    }

    /**
     * Register.
     *
     * @param watcher the watcher
     * @param events the events
     * @param modifiers the modifiers
     * @return the watch key
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return internalPath.register(watcher, events, modifiers);
    }

    /**
     * Register.
     *
     * @param watcher the watcher
     * @param events the events
     * @return the watch key
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return internalPath.register(watcher, events);
    }

    /**
     * Iterator.
     *
     * @return the iterator
     */
    @Override
    public Iterator<Path> iterator() {
        return internalPath.iterator();
    }

    /**
     * For each.
     *
     * @param action the action
     */
    @Override
    public void forEach(Consumer<? super Path> action) {
        internalPath.forEach(action);
    }

    /**
     * Spliterator.
     *
     * @return the spliterator
     */
    @Override
    public Spliterator<Path> spliterator() {
        return internalPath.spliterator();
    }

    /**
     * Compare to.
     *
     * @param other the other
     * @return the int
     */
    @Override
    public int compareTo(Path other) {
        return internalPath.compareTo(other);
    }
}
