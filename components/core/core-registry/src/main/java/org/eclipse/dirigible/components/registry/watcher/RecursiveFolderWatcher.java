/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
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

/**
 * Mirrors an external folder into a target folder - the registry - and keeps mirroring it as the
 * source changes.
 *
 * <p>
 * The copy writes onto the registry's own file system path, behind {@code IRepository} and behind
 * the publisher pipeline, so the platform learns nothing about it on its own: the registry watcher
 * that schedules synchronization passes registers only the registry root and never sees a file
 * written several folders deep. This class therefore reports its writes the way a publish does - it
 * brackets them with {@link RegistryMutationTracker} so a pass overlapping the copy defers its
 * cleanup instead of reaping artefacts whose sources have not arrived yet, and it marks the
 * registry modified once a batch of writes is applied so the next pass actually runs. Without the
 * latter, a pass that caught the initial copy half-way was the last one to ever run and whatever it
 * happened to see stayed the installed state for the life of the process.
 */
public class RecursiveFolderWatcher implements DisposableBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RecursiveFolderWatcher.class);

    /** How long {@link #destroy()} waits for the watch service's own close before walking away. */
    private static final long CLOSE_TIMEOUT_SECONDS = 2;

    private final Map<WatchKey, Path> keyToPathMap = new HashMap<>();

    /** Marks the windows in which this watcher is writing to the registry. */
    private final RegistryMutationTracker mutationTracker;

    /** Told that the registry changed, so a synchronization pass is scheduled. */
    private final SynchronizationWatcher synchronizationWatcher;

    private Set<String> ignoredFolders = Collections.emptySet();
    private Path sourceDir;
    private Path targetDir;
    private WatchService watchService;
    private ExecutorService executorService;

    /**
     * Whether the watch loop is up, i.e. a change made to the source folder from now on is seen. The
     * initial copy runs before the directories are registered, so this is the point from which the
     * mirroring is live - and the point a test has to wait for before changing the source.
     */
    private volatile boolean watching;

    /**
     * Instantiates a new recursive folder watcher.
     *
     * @param mutationTracker the registry mutation tracker
     * @param synchronizationWatcher the synchronization watcher
     */
    protected RecursiveFolderWatcher(RegistryMutationTracker mutationTracker, SynchronizationWatcher synchronizationWatcher) {
        this.mutationTracker = mutationTracker;
        this.synchronizationWatcher = synchronizationWatcher;
    }

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

    /**
     * Perform initial sync of all files and folders.
     *
     * <p>
     * The whole walk is one mutation window: it copies the tree file by file, and a synchronization
     * pass looking into it must not take the files that have not been copied yet for deleted sources.
     * The registry is marked modified once, when the tree is complete - the point at which a pass is
     * worth running.
     */
    private void initialSync() throws IOException {
        logger.info("Performing initial sync...");
        mutationTracker.enter();
        try {
            walkAndSync();
        } finally {
            mutationTracker.exit();
        }
        logger.info("Initial sync complete.");
        registryChanged();
    }

    private void walkAndSync() throws IOException {
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
    }

    /**
     * Tell the platform the registry changed, so the next scheduled synchronization pass actually runs.
     * Called once per applied batch of writes, not per file.
     */
    private void registryChanged() {
        logger.debug("The external registry folder changed - scheduling a synchronization pass");
        synchronizationWatcher.force();
    }

    private void syncFile(Path sourceFile) {
        if (Files.isDirectory(sourceFile) || isIgnored(sourceFile)) {
            return;
        }
        mutationTracker.enter();
        try {
            Path relative = sourceDir.relativize(sourceFile);
            Path targetFile = targetDir.resolve(relative);
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
            logger.info("Synced: " + sourceFile + " → " + targetFile);
        } catch (IOException e) {
            logger.error("Failed to sync: " + sourceFile, e);
        } finally {
            mutationTracker.exit();
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
        watching = true;

        while (true) {
            WatchKey key = watchService.take();
            Path dir = keyToPathMap.get(key);
            if (dir == null) {
                logger.error("WatchKey not recognized!");
                continue;
            }

            boolean changed = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW)
                    continue;

                Path name = (Path) event.context();
                Path sourcePath = dir.resolve(name);
                changed = true;

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

            if (changed) {
                registryChanged();
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

    /**
     * Whether the watch loop is up - see {@link #watching}.
     *
     * @return true once changes to the source folder are being mirrored
     */
    boolean isWatching() {
        return watching;
    }

    private void deleteFile(Path sourceFile) {
        mutationTracker.enter();
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
        } finally {
            mutationTracker.exit();
        }
    }

    /**
     * Destroy.
     *
     * <p>
     * The watch service is closed <b>off the caller's thread</b> for the reason documented on
     * {@link LocalRegistryWatcher#closeOffThread}: on a platform with no native file-event source the
     * JDK's polling watch service can deadlock against its own poller, and a teardown that waits for
     * that close never returns. Walking away costs nothing - the poller it leaves behind is a daemon
     * thread.
     */
    @Override
    public void destroy() {
        logger.info("Destroying Recursive Folder Watcher");

        watching = false;

        WatchService service = this.watchService;
        this.watchService = null;
        if (null != service && !LocalRegistryWatcher.closeOffThread(service, CLOSE_TIMEOUT_SECONDS)) {
            logger.warn("The External Registry watch service did not close within [{}]s - leaving it to the JVM", CLOSE_TIMEOUT_SECONDS);
        }

        if (null != executorService) {
            executorService.shutdownNow();
            executorService = null;
        }
    }
}
