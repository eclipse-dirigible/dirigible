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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Class LocalRegistryWatcher.
 */
@Component
@Scope("singleton")
public class LocalRegistryWatcher implements DisposableBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(LocalRegistryWatcher.class);

    /** How long destroy() waits for the watch loop to leave before closing the service. */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    /** The key to path map. */
    private final Map<WatchKey, Path> keyToPathMap = new HashMap<>();

    /** The ignored folders. */
    private Set<String> ignoredFolders = Collections.emptySet();

    /** The source dir. */
    private Path sourceDir;

    /** The watch service. */
    private WatchService watchService;

    /** The executor service. */
    private ExecutorService executorService;

    /**
     * Whether the watch loop should keep taking events. Cleared by {@link #destroy()} BEFORE the
     * service is closed, so the loop leaves on its own terms instead of being torn out from under a
     * blocking {@code take()} - see the shutdown-order note on {@link #destroy()}.
     */
    private volatile boolean watching;

    /**
     * The thread running the watch loop, kept so shutdown can be observed (and asserted) rather than
     * only assumed. Set when the loop starts, cleared when it leaves.
     */
    private volatile Thread watchThread;

    /**
     * How the watch loop last left: {@code interrupted} (shutdown asked it to stop - the correct
     * order), {@code closed} (the service was closed underneath a blocking {@code take()} - the order
     * that deadlocks on macOS), or {@code null} if it left on the cleared flag. Read by the shutdown
     * test, which is how "the loop was stopped BEFORE the service was closed" becomes an assertion
     * instead of a comment.
     */
    private volatile String lastExitCause;

    /** The repository. */
    private final IRepository repository;

    /** The handlers. */
    private final List<LocalRegistryWatcherHandler> handlers;

    /**
     * Instantiates a new local registry watcher.
     *
     * @param repository the repository
     * @param handlers the handlers
     */
    @Autowired
    public LocalRegistryWatcher(IRepository repository, List<LocalRegistryWatcherHandler> handlers) {
        this.repository = repository;
        this.handlers = handlers;
    }

    /**
     * Initialize.
     */
    public synchronized void initialize() {
        this.ignoredFolders = getIgnoredFolders();
        this.sourceDir = Paths.get(this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC))
                              .toAbsolutePath();

        logger.info("Initializing the Local Registry file watcher on [{}], ignoring {}...", sourceDir, this.ignoredFolders);

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                if (!Files.exists(sourceDir)) {
                    throw new IllegalArgumentException("Source folder does not exist: " + sourceDir);
                }

                if (this.watchService != null) {
                    logger.warn(
                            "Local Registry Watcher has been initialized already. Existing watcher will be closed and a new one will be created.");
                    // Only the service: this runs ON the watcher thread, so shutting the executor down
                    // and awaiting it (what destroy() does) would be this thread waiting for itself.
                    closeWatchService();
                }

                this.watchService = FileSystems.getDefault()
                                               .newWatchService();

                // Initial sync before start watching
                initialSync();

                // Register watchers recursively
                registerAll(sourceDir);

                // Start actual watching
                this.startWatching();
            } catch (IOException | InterruptedException e) {
                logger.error("Error during initializing the Local Registry Watcher", e);
            }
        });
        logger.debug("Done initializing the Local Registry file watcher.");
    }

    /**
     * Gets the ignored folders.
     *
     * @return the ignored folders
     */
    private Set<String> getIgnoredFolders() {
        String ignoredFolders = DirigibleConfig.REGISTRY_LOCAL_IGNORED_FOLDERS.getStringValue();
        if (null == ignoredFolders) {
            return Collections.emptySet();
        }
        String[] folders = ignoredFolders.split(",");
        return Arrays.stream(folders)
                     .map(String::trim)
                     .map(LocalRegistryWatcher::sanitizeFolderName)
                     .collect(Collectors.toSet());

    }

    /**
     * Sanitizes a folder name loaded from configuration to prevent log injection.
     *
     * @param folderName the original folder name
     * @return the sanitized folder name
     */
    private static String sanitizeFolderName(String folderName) {
        if (folderName == null) {
            return null;
        }
        // Remove carriage return and newline characters to prevent log forging
        return folderName.replace("\r", "")
                         .replace("\n", "");
    }

    /**
     * Perform initial sync of all files and folders.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void initialSync() throws IOException {
        logger.info("Performing initial sync...");
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(dir)) {
                    logger.debug("Skipping ignored directory: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                directoryRegistered(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!isIgnored(file)) {
                    fileRegistered(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        logger.info("Initial sync complete.");
    }

    /**
     * Checks if is ignored.
     *
     * @param path the path
     * @return true, if is ignored
     */
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

    /**
     * Register directory and all sub-directories.
     *
     * @param start the start
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void registerAll(final Path start) throws IOException {
        register(start);
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

    /**
     * Register single directory.
     *
     * @param dir the dir
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        keyToPathMap.put(key, dir);
    }

    /**
     * Start watching.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    public void startWatching() throws IOException, InterruptedException {
        logger.info("Recursively watching: " + sourceDir);

        watching = true;
        watchThread = Thread.currentThread();
        try {
            watchLoop();
        } finally {
            watching = false;
            watchThread = null;
        }
    }

    /**
     * Whether the watch loop is still running. Package-private: the shutdown test asserts the loop is
     * actually gone after {@link #destroy()} instead of trusting that it is.
     *
     * @return true while the watch thread is inside the loop
     */
    boolean isWatching() {
        Thread thread = watchThread;
        return watching && thread != null && thread.isAlive();
    }

    /**
     * How the watch loop last left. Package-private for the shutdown test.
     *
     * @return {@code interrupted}, {@code closed}, or {@code null} - see {@link #lastExitCause}
     */
    String getLastExitCause() {
        return lastExitCause;
    }

    /**
     * The event loop itself. Leaves on a cleared {@code watching} flag, on an interrupt, or on a closed
     * service - so a shutdown never has to tear the loop out of a blocking {@code take()}.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void watchLoop() throws IOException {
        while (watching) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (ClosedWatchServiceException | InterruptedException e) {
                // The normal way out: destroy() cleared `watching` and interrupted this thread (or, in
                // the legacy order, closed the service under us). Either way there is nothing left to
                // watch - leave the loop so the service can be closed with no poller inside it.
                lastExitCause = e instanceof InterruptedException ? "interrupted" : "closed";
                logger.debug("Local Registry Watcher stopped watching ({}): {}", lastExitCause, e.getMessage());
                return;
            }
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
                                    fileCreated(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                    directoryCreated(dir);
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            logger.error("Failed to sync new folder: " + sourcePath, e);
                        }
                    } else {
                        fileCreated(sourcePath);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (!Files.isDirectory(sourcePath)) {
                        fileModified(sourcePath);
                    }
                } else if (kind == ENTRY_DELETE) {
                    fileDeleted(sourcePath);
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

    /**
     * Directory registered.
     *
     * @param path the path
     */
    private void directoryRegistered(Path path) {
        if (!Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.directoryRegistered(path);
            } catch (Exception e) {
                logger.error("Failed to handle registration of a directory: " + path, e);
            }
        }
    }

    /**
     * File registered.
     *
     * @param path the path
     */
    private void fileRegistered(Path path) {
        if (Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.fileRegistered(path);
            } catch (Exception e) {
                logger.error("Failed to handle registration of a file: " + path, e);
            }
        }
    }

    /**
     * Directory created.
     *
     * @param path the path
     */
    private void directoryCreated(Path path) {
        if (!Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.directoryCreated(path);
            } catch (Exception e) {
                logger.error("Failed to handle creation of a directory: " + path, e);
            }
        }
    }

    /**
     * File created.
     *
     * @param path the path
     */
    private void fileCreated(Path path) {
        if (Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.fileCreated(path);
            } catch (Exception e) {
                logger.error("Failed to handle creation of a file: " + path, e);
            }
        }
    }

    /**
     * File modified.
     *
     * @param path the path
     */
    private void fileModified(Path path) {
        if (Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.fileModified(path);
            } catch (Exception e) {
                logger.error("Failed to handle modification of a file: " + path, e);
            }
        }
    }

    /**
     * File deleted.
     *
     * @param path the path
     */
    private void fileDeleted(Path path) {
        if (Files.isDirectory(path) || isIgnored(path)) {
            return;
        }
        for (LocalRegistryWatcherHandler handler : handlers) {
            try {
                handler.fileDeleted(path);
            } catch (Exception e) {
                logger.error("Failed to handle deletion of a file: " + path, e);
            }
        }
    }

    /**
     * Destroy.
     *
     * <p>
     * <b>Order matters: stop the watching thread FIRST, close the service second.</b> Closing the
     * service while the thread sits in {@code take()} deadlocks on macOS, where the JDK has no native
     * file-event source and falls back to {@code sun.nio.fs.PollingWatchService}: its {@code close()}
     * walks the registered keys and locks each one while holding the key map, whereas the polling
     * thread locks a key first and then the map - a lock-order inversion, and Spring's singleton
     * teardown hangs for good (every {@code @DirtiesContext} integration test on a developer's Mac).
     * Linux and Windows have native watchers and never showed it.
     *
     * <p>
     * So: clear {@code watching}, {@code shutdownNow()} to interrupt the blocking {@code take()} (the
     * loop returns on the {@code InterruptedException}), wait briefly for the thread to actually be
     * gone, and only then close the service - by which point no one is inside it.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void destroy() throws IOException {
        logger.info("Destroying Local Registry Watcher");

        watching = false;

        ExecutorService executor = this.executorService;
        this.executorService = null;
        if (null != executor) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    // Not fatal: the watch loop only reads the file system, so a straggler cannot
                    // corrupt anything. Say so instead of closing the service on top of it silently.
                    logger.warn("The Local Registry Watcher thread did not stop within [{}]s - closing the watch service anyway",
                            SHUTDOWN_TIMEOUT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                logger.warn("Interrupted while waiting for the Local Registry Watcher thread to stop", e);
            }
        }

        closeWatchService();
    }

    /**
     * Close the watch service, if any. Split out of {@link #destroy()} because the re-initialization
     * path runs on the watcher thread itself and must not shut down the executor it is running in.
     *
     * @throws IOException if the close fails
     */
    private void closeWatchService() throws IOException {
        WatchService service = this.watchService;
        this.watchService = null;
        if (null != service) {
            service.close();
        }
    }
}
