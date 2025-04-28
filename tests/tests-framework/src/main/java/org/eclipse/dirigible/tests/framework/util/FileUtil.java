/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private static final String FILE_PREFIX = "file:";

    public static List<Path> findFiles(File folder, String fileExtension) throws IOException {
        return findFiles(folder.toPath(), fileExtension);
    }

    public static List<Path> findFiles(Path path, String fileExtension) throws IOException {
        return findFiles(path).stream()
                              .filter(f -> f.toString()
                                            .toLowerCase()
                                            .endsWith(fileExtension))
                              .collect(Collectors.toList());
    }

    public static List<Path> findFiles(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            LOGGER.debug("Folder [{}] doesn't exist", folder);
            return Collections.emptyList();
        }
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Path [" + folder + "] must be a directory");
        }

        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(p -> !Files.isDirectory(p))
                       .collect(Collectors.toList());
        }
    }

    public static void deleteFolder(String folderPath) {
        File folder = new File(folderPath);
        deleteFolder(folder);
    }

    public static void deleteFolder(File folder) {
        if (folder.exists()) {
            Awaitility.await()
                      .atMost(15, TimeUnit.SECONDS)
                      .until(() -> {
                          return deleteFolderResursively(folder);
                      });
        }
    }

    private static boolean deleteFolderResursively(File folder) {
        LOGGER.debug("Will delete folder recursively [{}]", folder);
        boolean deleted = FileSystemUtils.deleteRecursively(folder);
        LOGGER.debug("Deleted folder [{}] recursively: [{}]", folder, deleted);
        return deleted;
    }

    public static void deleteFolder(String folderPath, String skippedDirPath) {
        LOGGER.info("Deleting folder [{}] by skipping [{}]...", folderPath, skippedDirPath);
        try {
            Path baseDir = Paths.get(folderPath);
            Path excludeDir = Paths.get(skippedDirPath);

            if (Files.exists(baseDir)) {
                Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        boolean subdir = dir.startsWith(excludeDir);
                        if (subdir) {
                            LOGGER.debug("Folder [{}] will not be deleted since it is subfolder of [{}]", dir, excludeDir);
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        boolean parentDir = excludeDir.startsWith(dir);
                        if (parentDir) {
                            LOGGER.debug("Folder [{}] will not be deleted since it is parent of [{}]", dir, excludeDir);
                            return FileVisitResult.CONTINUE;
                        }

                        deleteFolderResursively(dir.toFile());
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete dir " + folderPath + " by skipping " + skippedDirPath, ex);
        }
    }

    public static List<Path> findFiles(String folder) throws IOException {
        return findFiles(Path.of(folder));
    }

    public static void deleteFile(String filePath) {
        String path =
                filePath.startsWith(FILE_PREFIX) ? filePath.substring(filePath.indexOf(FILE_PREFIX) + FILE_PREFIX.length()) : filePath;
        File file = new File(path);
        LOGGER.debug("Will delete file [{}]", file);
        boolean deleted = file.delete();
        LOGGER.debug("Deleted file [{}]: [{}]", file, deleted);
        if (!deleted) {
            throw new IllegalStateException("Failed to delete file: " + file);
        }
    }
}
