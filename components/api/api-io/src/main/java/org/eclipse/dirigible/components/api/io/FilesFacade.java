/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Facade for working with files.
 */
@Component
public class FilesFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(FilesFacade.class);

    /**
     * Check if file with the provided path exists.
     *
     * @param path the path to the file
     * @return true if a file exists
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean exists(String path) throws IOException {
        // return Files.exists(Paths.get(path));
        return Paths.get(path)
                    .toFile()
                    .exists();
    }

    /**
     * Check if file with the provided path is executable.
     *
     * @param path the path to the file
     * @return true if the file is executable
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isExecutable(String path) throws IOException {
        return Files.isExecutable(Paths.get(path));
    }

    /**
     * Check if file with the provided path is readable.
     *
     * @param path the path to the file
     * @return true if the file is readable
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isReadable(String path) throws IOException {
        return Files.isReadable(Paths.get(path));
    }

    /**
     * Check if file with the provided path is writable.
     *
     * @param path the path to the file
     * @return true if the file is writable
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isWritable(String path) throws IOException {
        return Files.isReadable(Paths.get(path));
    }

    /**
     * Check if file with the provided path is hidden.
     *
     * @param path the path to the file
     * @return true if the file is hidden
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isHidden(String path) throws IOException {
        return Files.isHidden(Paths.get(path));
    }

    /**
     * Check if file with the provided path is a directory.
     *
     * @param path the path to the file
     * @return true if the file is directory
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isDirectory(String path) throws IOException {
        return Files.isDirectory(Paths.get(path));
    }

    /**
     * Check if file with the provided path is a regular file.
     *
     * @param path the path to the file
     * @return true if the file is a regular file
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isFile(String path) throws IOException {
        return Files.isRegularFile(Paths.get(path));
    }

    /**
     * Check if the two paths point to the same file.
     *
     * @param path1 path to the first file
     * @param path2 path to the second file
     * @return true if both paths point to the same file
     * @throws IOException in case of failure in underlying layer
     */
    public static final boolean isSameFile(String path1, String path2) throws IOException {
        return Files.isSameFile(Paths.get(path1), Paths.get(path2));
    }

    /**
     * Get canonical representation of the provided path.
     *
     * @param path the path
     * @return the canonical representation of the path
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getCanonicalPath(String path) throws IOException {
        return new File(path).getCanonicalPath();
    }

    /**
     * Get the name of the file or directory represented by the provided path.
     *
     * @param path the path
     * @return the name of the file or the directory
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getName(String path) throws IOException {
        return new File(path).getName();
    }

    /**
     * Get the path of the parent directory.
     *
     * @param path the path
     * @return the path of the parent
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getParentPath(String path) throws IOException {
        return new File(path).getParentFile()
                             .getPath(); // FIXME may throw NPE
    }

    /**
     * Read the content of the file into a byte array.
     *
     * @param path the path of the file
     * @return the file content as byte array
     * @throws IOException in case of failure in underlying layer
     */
    public static final byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    /**
     * Read the text file represented by the provided path using UTF-8 charset.
     *
     * @param path path to the file
     * @return the text file content
     * @throws IOException in case of failure in underlying layer
     */
    public static final String readText(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    /**
     * Write the provided data to the file represented by the provided path. If the file exists the old
     * content is discarded.
     *
     * @param path path to the file to write to
     * @param input the data to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void writeBytes(String path, String input) throws IOException {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        Files.write(Paths.get(path), bytes);
    }

    /**
     * Write the provided data to the file represented by the provided path. If the file exists the old
     * content is discarded.
     *
     * @param path path to the file to write to
     * @param input the data to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void writeBytesNative(String path, byte[] input) throws IOException {
        Files.write(Paths.get(path), input);
    }

    /**
     * Write text to the file represented by the provided path using UTF-8 charset. If the file exists
     * the old content is discarded.
     *
     * @param path path to the file to write to
     * @param text the data to write
     * @throws IOException in case of failure in underlying layer
     */
    public static final void writeText(String path, String text) throws IOException {
        Files.write(Paths.get(path), text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get the timestamp of last modification.
     *
     * @param path path to the file
     * @return the timestamp of last modification
     * @throws IOException in case of failure in underlying layer
     */
    public static final long getLastModified(String path) throws IOException {
        return Files.getLastModifiedTime(Paths.get(path))
                    .toMillis();
    }

    /**
     * Set the last modification timestamp.
     *
     * @param path path to a file
     * @param time the last modification timestamp to set
     * @throws IOException in case of failure in underlying layer
     */
    public static final void setLastModified(String path, long time) throws IOException {
        Files.setLastModifiedTime(Paths.get(path), FileTime.fromMillis(time));
    }

    /**
     * Set the last modification timestamp.
     *
     * @param path path to a file
     * @param time the last modification timestamp to set
     * @throws IOException in case of failure in underlying layer
     */
    public static final void setLastModified(String path, Double time) throws IOException {
        setLastModified(path, time.longValue());
    }

    /**
     * Get the name of teh principal representing the file owner.
     *
     * @param path path to a file
     * @return the name of the principal representing the file owner
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getOwner(String path) throws IOException {
        return Files.getOwner(Paths.get(path))
                    .getName();
    }

    /**
     * Set the file owner.
     *
     * @param path path to file
     * @param owner the owner
     * @throws IOException in case of failure in underlying layer
     */
    public static final void setOwner(String path, String owner) throws IOException {
        UserPrincipal userPrincipal = Paths.get(path)
                                           .getFileSystem()
                                           .getUserPrincipalLookupService()
                                           .lookupPrincipalByName(owner);
        Files.setOwner(Paths.get(path), userPrincipal);
    }

    /**
     * Get the string representation of the file permissions.
     *
     * @param path path to the file
     * @return the string representation of the file permissions
     * @throws IOException in case of failure in underlying layer
     */
    public static final String getPermissions(String path) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(Paths.get(path)));
    }

    /**
     * Set the file permissions.
     *
     * @param path path to the file
     * @param permissions the permissions string
     * @throws IOException in case of failure in underlying layer
     */
    public static final void setPermissions(String path, String permissions) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);
        Files.setPosixFilePermissions(Paths.get(path), perms);
    }

    /**
     * Get the file size.
     *
     * @param path path to the file
     * @return the file size
     * @throws IOException in case of failure in underlying layer
     */
    public static final long size(String path) throws IOException {
        return Files.size(Paths.get(path));
    }

    /**
     * Create a new file if it does not exist.
     *
     * @param path path to the file
     * @throws IOException in case of failure in underlying layer
     */
    public static final void createFile(String path) throws IOException {
        Files.createFile(Paths.get(path));
    }

    /**
     * Create all directories on the given path if they do not exist.
     *
     * @param path the path parameter
     * @throws IOException in case of failure in underlying layer
     */
    public static final void createDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    /**
     * Copy the directory structure.
     *
     * @param source source location
     * @param target target location
     * @throws IOException in case of failure in underlying layer
     */
    public static final void copy(String source, String target) throws IOException {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(sourcePath, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Move the file to the specified location.
     *
     * @param source source location
     * @param target target location
     * @throws IOException in case of failure in underlying layer
     */
    public static final void move(String source, String target) throws IOException {
        Files.move(Paths.get(source), Paths.get(target));
    }

    /**
     * Delete the file if it exists.
     *
     * @param path path to file
     * @throws IOException in case of failure in underlying layer
     */
    public static final void deleteFile(String path) throws IOException {
        Files.deleteIfExists(Paths.get(path));
    }

    /**
     * Delete empty directory.
     *
     * @param path path to the directory
     * @throws IOException in case of failure in underlying layer
     */
    public static final void deleteDirectory(String path) throws IOException {
        Files.deleteIfExists(Paths.get(path));
    }

    /**
     * Delete directory. Set foce to true in order to delete non empty directory
     *
     * @param path path to the directory
     * @param forced should non-empty directories be deleted
     * @throws IOException in case of failure in underlying layer
     */
    public static final void deleteDirectory(String path, boolean forced) throws IOException {
        if (forced) {
            EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            Files.walkFileTree(Paths.get(path), opts, Integer.MAX_VALUE, new FileVisitor<Path>() {

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

                    // if (Files.exists(dir)) {
                    if (dir.toFile()
                           .exists()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Deleting directory: %s", dir));
                        }
                        try {
                            Files.delete(dir);
                        } catch (java.nio.file.NoSuchFileException e) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("Directory already has been deleted: %s", dir));
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // if (Files.exists(file)) {
                    if (file.toFile()
                            .exists()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("Deleting file: %s", file));
                        }
                        try {
                            Files.delete(file);
                        } catch (java.nio.file.NoSuchFileException e) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(String.format("File already has been deleted: %s", file));
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    if (logger.isErrorEnabled()) {
                        logger.error(String.format("Error in file: %s", file), exc);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.deleteIfExists(Paths.get(path));
        }
    }

    /**
     * Create temporary file.
     *
     * @param prefix the prefix parameter
     * @param suffix the suffix parameter
     * @return path to the temp file
     * @throws IOException in case of failure in underlying layer
     */
    public static final String createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix)
                    .toString();
    }

    /**
     * Create temporary directory.
     *
     * @param prefix the prefix parameter
     * @return path to the created directory
     * @throws IOException in case of failure in underlying layer
     */
    public static final String createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix)
                    .toString();
    }


    /**
     * Open input stream from the file represented by the given path.
     *
     * @param path path to file
     * @return the created input stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final InputStream createInputStream(String path) throws IOException {
        return Files.newInputStream(Paths.get(path));
    }

    /**
     * Open output stream to the file represented by the given path.
     *
     * @param path path to file
     * @return the created output stream
     * @throws IOException in case of failure in underlying layer
     */
    public static final OutputStream createOutputStream(String path) throws IOException {
        return Files.newOutputStream(Paths.get(path));
    }

    /**
     * Traverse the directory structure.
     *
     * @param source source location
     * @return JSON rendered files and folders tree
     * @throws IOException in case of failure in underlying layer
     */
    public static final String traverse(String source) throws IOException {
        Path sourcePath = Paths.get(source);
        List<FolderObject> root = new ArrayList<>();
        Map<Path, FolderObject> map = new HashMap<>();
        EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(sourcePath, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

            FolderObject currentFolder = null;

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                if (!map.containsKey(dir)) {
                    currentFolder = new FolderObject(dir.getFileName()
                                                        .toString(),
                            dir.toString(), "folder");
                    map.put(dir, currentFolder);
                    if (map.containsKey(dir.getParent())) {
                        map.get(dir.getParent())
                           .getFolders()
                           .add(currentFolder);
                    }
                }
                if (root.isEmpty()) {
                    root.add(currentFolder);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                FileObject fileObject = new FileObject(file.getFileName()
                                                           .toString(),
                        file.toString(), "file");
                currentFolder.getFiles()
                             .add(fileObject);
                return FileVisitResult.CONTINUE;
            }
        });

        return GsonHelper.toJson(root.iterator()
                                     .next());
    }

    /**
     * List the directory structure.
     *
     * @param source source location
     * @return JSON rendered files and folders list
     * @throws IOException in case of failure in underlying layer
     */
    public static final String list(String source) throws IOException {
        File[] listFiles = FileSystemUtils.listFiles(new File(source));
        List<FileObject> files = new ArrayList<FileObject>();
        for (File next : listFiles) {
            files.add(new FileObject(next.getName(), next.getPath(), null));
        }
        return GsonHelper.toJson(files);
    }

    /**
     * Find all the files matching the pattern.
     *
     * @param path the root path
     * @param pattern the glob pattern
     * @return the list of file names
     * @throws IOException in case of an error
     */
    public static String find(String path, String pattern) throws IOException {
        return GsonHelper.toJson(FileSystemUtils.find(path, pattern));
    }

}
