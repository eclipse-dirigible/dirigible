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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The Class ZipProcessor.
 */
@Component
public class ZipProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ZipProcessor.class);

    /**
     * Zip.
     *
     * @param path the path
     * @param zipTargetPath the zip target path
     */
    public static void zip(String path, String zipTargetPath) {
        Path source = Paths.get(path);
        Path target = Paths.get(zipTargetPath);
        try {
            zipFolder(source, target);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Unzip.
     *
     * @param zipPath the zip path
     * @param targetPath the target path
     */
    public static void unzip(String zipPath, String targetPath) {
        Path source = Paths.get(zipPath);
        Path target = Paths.get(targetPath);
        try {
            unzipFolder(source, target);
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }

    }

    /**
     * Unzip folder.
     *
     * @param source the source
     * @param target the target
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void unzipFolder(Path source, Path target) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                boolean isDirectory = zipEntry.getName()
                                              .endsWith(File.separator);
                Path newPath = zipSlipProtect(zipEntry, target);
                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

    }


    /**
     * Zip folder.
     *
     * @param sourceFolderPath the source folder path
     * @param zipPath the zip path
     * @throws Exception the exception
     */
    private static void zipFolder(Path sourceFolderPath, Path zipPath) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file)
                                                                  .toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Zip slip protect.
     *
     * @param zipEntry the zip entry
     * @param targetDir the target dir
     * @return the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    // protect zip slip attack
    public static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }

}
