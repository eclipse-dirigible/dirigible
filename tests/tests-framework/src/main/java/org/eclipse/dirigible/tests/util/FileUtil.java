/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.util;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

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
            LOGGER.info("Folder [{}] doesn't exist", folder);
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
            LOGGER.info("Will delete folder [{}]", folder);
            Awaitility.await()
                      .atMost(15, TimeUnit.SECONDS)
                      .until(() -> FileSystemUtils.deleteRecursively(folder));
        }
    }

    public static List<Path> findFiles(String folder) throws IOException {
        return findFiles(Path.of(folder));
    }
}
