/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.*;

public class RecursiveFolderWatcher implements DisposableBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RecursiveFolderWatcher.class);

    private final Map<WatchKey, Path> keyToPathMap = new HashMap<>();
    private Set<String> ignoredFolders = Collections.emptySet();
    private Path sourceDir;
    private Path targetDir;
    private WatchService watchService;
    private ExecutorService executorService;

    /**
     * Initialize.
     */
    public synchronized void initialize(String source, String target, boolean sourceAsSubfolder, Set<String> ignoredFolders) {
        this.ignoredFolders = ignoredFolders;
        this.sourceDir = Paths.get(source)
                              .toAbsolutePath();
        if (sourceAsSubfolder) {
            String sourceFolderName = this.sourceDir.getFileName()
                                                    .toString();
            this.targetDir = Paths.get(target, sourceFolderName)
                                  .toAbsolutePath();
        } else {
            this.targetDir = Paths.get(target)
                                  .toAbsolutePath();
        }

        logger.info("Initializing the External Registry file watcher from [{}] to [{}], ignoring {}...", sourceDir, targetDir,
                this.ignoredFolders);

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                if (!Files.exists(sourceDir)) {
                    throw new IllegalArgumentException("Source folder does not exist: " + sourceDir);
                }
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                if (this.watchService != null) {
                    logger.warn(
                            "Recursive Folder Watcher has been initialized already. Existing watcher will be closed and a new one will be created.");
                    destroy();
                }

                this.watchService = FileSystems.getDefault()
                                               .newWatchService();

                // Initial sync before watching
                initialSync();

                // Register watchers recursively
                registerAll(sourceDir);

                // Start actual watching
                this.startWatching();
            } catch (IOException | InterruptedException e) {
                logger.error("Error during initializing the External Registry Watcher", e);
            }
        });
        logger.debug("Done initializing the External Registry file watcher.");
    }

    /** Perform initial sync of all files and folders */
    private void initialSync() throws IOException {
        logger.info("Performing initial sync...");
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(dir)) {
                    logger.debug("Skipping ignored directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path relative = sourceDir.relativize(dir);
                Path targetSubDir = targetDir.resolve(relative);
                Files.createDirectories(targetSubDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!isIgnored(file)) {
                    syncFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        logger.info("Initial sync complete.");
    }

    private void syncFile(Path sourceFile) {
        try {
            if (Files.isDirectory(sourceFile) || isIgnored(sourceFile)) {
                return;
            }
            Path relative = sourceDir.relativize(sourceFile);
            Path targetFile = targetDir.resolve(relative);
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
            logger.info("Synced: " + sourceFile + " → " + targetFile);
        } catch (IOException e) {
            logger.error("Failed to sync: " + sourceFile, e);
        }
    }

    private boolean isIgnored(Path path) {
        if (ignoredFolders.isEmpty()) {
            return false;
        }

        // Only ignore if this is a top-level folder directly under sourceDir
        Path relative = sourceDir.relativize(path);
        if (relative.getNameCount() == 1) { // path is immediate child of sourceDir

            String topName = relative.getFileName()
                                     .toString();

            for (String ignored : ignoredFolders) {
                if (topName.equalsIgnoreCase(ignored)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Register directory and all sub-directories */
    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(dir)) {
                    logger.debug("Skipping ignored directory registration: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Register single directory */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        keyToPathMap.put(key, dir);
    }

    public void startWatching() throws IOException, InterruptedException {
        logger.info("Recursively watching: " + sourceDir);

        while (true) {
            WatchKey key = watchService.take();
            Path dir = keyToPathMap.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW)
                    continue;

                Path name = (Path) event.context();
                Path sourcePath = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(sourcePath)) {
                        // Register new directory
                        registerAll(sourcePath);
                        // Also sync its contents
                        try {
                            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                    syncFile(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
                                    Path relative = sourceDir.relativize(d);
                                    Path targetSubDir = targetDir.resolve(relative);
                                    Files.createDirectories(targetSubDir);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            logger.error("Failed to sync new folder: " + sourcePath, e);
                        }
                    } else {
                        syncFile(sourcePath);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    syncFile(sourcePath);
                } else if (kind == ENTRY_DELETE) {
                    deleteFile(sourcePath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keyToPathMap.remove(key);
                if (keyToPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void deleteFile(Path sourceFile) {
        try {
            Path relative = sourceDir.relativize(sourceFile);
            Path targetFile = targetDir.resolve(relative);

            if (Files.isDirectory(targetFile)) {
                // Recursively delete directory and contents
                Files.walkFileTree(targetFile, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                logger.info("Deleted directory: " + targetFile);
            } else {
                Files.deleteIfExists(targetFile);
                logger.info("Deleted file: " + targetFile);
            }
        } catch (IOException e) {
            logger.error("Failed to delete: " + sourceFile, e);
        }
    }

    @Override
    public void destroy() throws IOException {
        logger.info("Destroying Recursive Folder Watcher");

        if (null != watchService) {
            watchService.close();
            watchService = null;
        }

        if (null != executorService) {
            executorService.shutdown();
            executorService = null;
        }
    }
}
