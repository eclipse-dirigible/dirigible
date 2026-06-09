/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.io;

import java.io.IOException;
import org.eclipse.dirigible.components.api.io.FilesFacade;

/**
 * Filesystem operations against the operating-system view — read/write content, query metadata,
 * change permissions, copy or move entries. Paths are absolute OS paths (not registry paths); to
 * read or write artefacts under {@code /registry/public/...} or a project workspace use
 * {@link org.eclipse.dirigible.sdk.platform.Registry Registry} or
 * {@link org.eclipse.dirigible.sdk.platform.Workspace Workspace} instead.
 * <p>
 * Common patterns:
 *
 * <pre>
 * String contents = Files.readText("/var/data/import.csv");
 * Files.writeText("/var/data/out.json", json);
 * if (!Files.exists(target)) {
 *     Files.createDirectory(target);
 * }
 * </pre>
 *
 * Every method propagates {@link IOException} from the platform — callers should funnel them up to
 * a controller or background-job boundary that knows what to do with them.
 */
public final class Files {

    private Files() {}

    public static boolean exists(String path) throws IOException {
        return FilesFacade.exists(path);
    }

    public static boolean isExecutable(String path) throws IOException {
        return FilesFacade.isExecutable(path);
    }

    public static boolean isReadable(String path) throws IOException {
        return FilesFacade.isReadable(path);
    }

    public static boolean isWritable(String path) throws IOException {
        return FilesFacade.isWritable(path);
    }

    public static boolean isHidden(String path) throws IOException {
        return FilesFacade.isHidden(path);
    }

    public static boolean isDirectory(String path) throws IOException {
        return FilesFacade.isDirectory(path);
    }

    public static boolean isFile(String path) throws IOException {
        return FilesFacade.isFile(path);
    }

    public static boolean isSameFile(String path1, String path2) throws IOException {
        return FilesFacade.isSameFile(path1, path2);
    }

    public static String getCanonicalPath(String path) throws IOException {
        return FilesFacade.getCanonicalPath(path);
    }

    public static String getName(String path) throws IOException {
        return FilesFacade.getName(path);
    }

    public static String getParentPath(String path) throws IOException {
        return FilesFacade.getParentPath(path);
    }

    public static byte[] readBytes(String path) throws IOException {
        return FilesFacade.readBytes(path);
    }

    public static String readText(String path) throws IOException {
        return FilesFacade.readText(path);
    }

    public static void writeText(String path, String text) throws IOException {
        FilesFacade.writeText(path, text);
    }

    public static void writeBytes(String path, String input) throws IOException {
        FilesFacade.writeBytes(path, input);
    }

    public static void writeBytesNative(String path, byte[] input) throws IOException {
        FilesFacade.writeBytesNative(path, input);
    }

    public static long getLastModified(String path) throws IOException {
        return FilesFacade.getLastModified(path);
    }

    public static void setLastModified(String path, long time) throws IOException {
        FilesFacade.setLastModified(path, time);
    }

    public static String getOwner(String path) throws IOException {
        return FilesFacade.getOwner(path);
    }

    public static void setOwner(String path, String owner) throws IOException {
        FilesFacade.setOwner(path, owner);
    }

    public static String getPermissions(String path) throws IOException {
        return FilesFacade.getPermissions(path);
    }

    public static void setPermissions(String path, String permissions) throws IOException {
        FilesFacade.setPermissions(path, permissions);
    }

    public static long size(String path) throws IOException {
        return FilesFacade.size(path);
    }

    public static void createFile(String path) throws IOException {
        FilesFacade.createFile(path);
    }

    public static void createDirectory(String path) throws IOException {
        FilesFacade.createDirectory(path);
    }

    public static void copy(String source, String target) throws IOException {
        FilesFacade.copy(source, target);
    }

    public static void move(String source, String target) throws IOException {
        FilesFacade.move(source, target);
    }

    public static void deleteFile(String path) throws IOException {
        FilesFacade.deleteFile(path);
    }
}
