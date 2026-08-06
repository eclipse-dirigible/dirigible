/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript.transpilation;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Order(ApplicationListenersOrder.ApplicationReadyEventListeners.TYPE_SCRIPT_TRANSPILATION_SERVICE)
@Component
@ConditionalOnExpression("'${DIRIGIBLE_TSC_WATCH_SERVICE_ENABLED:true}'.toLowerCase() != 'false'")
public class TscWatcherService implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TscWatcherService.class);

    // Matches terminal control characters and ANSI sequences that can clear or move the console cursor:
    // \r - carriage return (rewrites current line)
    // \u0007 - bell character (beep)
    // \u001B[2J - clear screen
    // \u001B[H - move cursor to home position (top-left)
    // These are stripped from tsc output to prevent log lines from disappearing or refreshing.
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\r\\u0007\\u001B\\[2J\\u001B\\[H]");

    /** How many times to re-attempt the registry folder creation when it races a concurrent change. */
    private static final int CREATE_FOLDER_ATTEMPTS = 3;

    private static final String TS_CONFIG_CONTENT = """
            {
                "compilerOptions": {
                    "module": "ESNext",
                    "target": "ES2022",
                    "rootDir": "./",
                    "esModuleInterop": true,
                    "moduleResolution": "bundler",
                    "baseUrl": "./",
                    "lib": [
                        "ESNext",
                        "DOM"
                    ],
                    "paths": {
                        "@aerokit/sdk/*": [
                            "./modules/src/*"
                        ],
                        "sdk/*": [
                            "./modules/src/*"
                        ],
                        "/*": [
                            "./*"
                        ]
                    },
                    "types": [
                        "./modules/types"
                    ],
                    "strict": false,
                    "noImplicitAny": false,
                    "strictNullChecks": false,
                    "noImplicitThis": false,
                    "skipLibCheck": true
                },
                "exclude": ["modules", "modules-tests"]
            }
            """;
    private final IRepository repository;
    private ExecutorService executor;
    private Process tscProcess;

    TscWatcherService(IRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "TscWatcherService{" + "repository=" + repository + ", executor=" + executor + ", tscProcess=" + tscProcess + '}';
    }

    /**
     * Checks every 30 seconds if the tsc process is alive. If not, restarts it.
     */
    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void monitorTscProcess() {
        if (tscProcess == null || !tscProcess.isAlive()) {
            LOGGER.warn("tsc watch service is not initialized or it is not alive. Will start it again.");
            restart();
        }
    }

    public synchronized void restart() {
        LOGGER.info("Restarting {}...", this);
        destroy();

        start();
    }

    private synchronized void start() {
        try {
            createOrReplaceTsConfig();
            startTscWatch();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to start tsc watch service", ex);
        }
    }

    private void createOrReplaceTsConfig() {
        Path registryFolderPath = getRegistryFolderPath();

        Path tsConfigPath = registryFolderPath.resolve("tsconfig.json");

        LOGGER.info("Creating tsconfig.json file with path [{}]", tsConfigPath);
        LOGGER.debug("tsconfig.json content:\n{}", TS_CONFIG_CONTENT);
        try {
            createRegistryFolder(registryFolderPath);
            Files.writeString(tsConfigPath, TS_CONFIG_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create registry tsconfig.json with path [" + tsConfigPath + "]", ex);
        }
    }

    private Path getRegistryFolderPath() {
        String path = this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        return Path.of(path);
    }

    private void startTscWatch() {
        if (tscProcess != null && tscProcess.isAlive()) {
            LOGGER.info("TSC watch process is already running and will not be retriggered. Process [{}]", tscProcess);
            return;
        }

        this.executor = Executors.newFixedThreadPool(2);

        Path registryFolderPath = getRegistryFolderPath();
        createDir(registryFolderPath);
        try {
            LOGGER.info("Starting tsc watch in dir [{}]...", registryFolderPath);

            // ProcessBuilder on Windows does not always fully respect the PATHEXT variable
            // when resolving a bare command name (like "tsc") unless it's an .exe
            String tscCmd = SystemUtils.IS_OS_WINDOWS ? "tsc.cmd" : "tsc";

            ProcessBuilder processBuilder = new ProcessBuilder(tscCmd, "--watch", "--pretty", "false");
            processBuilder.directory(registryFolderPath.toFile());
            processBuilder.redirectErrorStream(false); // keep stdout/stderr separate

            tscProcess = processBuilder.start();

            // STDOUT -> LOGGER.INFO
            executor.submit(() -> streamToLogger(tscProcess.getInputStream(), false));

            // STDERR -> LOGGER.ERROR
            executor.submit(() -> streamToLogger(tscProcess.getErrorStream(), true));

        } catch (IOException ex) {
            throw new IllegalStateException("Failed to start tsc watch in registry folder " + registryFolderPath, ex);
        }
    }

    private void streamToLogger(InputStream inputStream, boolean isError) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 64 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {

                // remove control chars to prevent console modifications
                String escapedLine = CONTROL_CHARS.matcher(line)
                                                  .replaceAll("");

                if (isError) {
                    LOGGER.error("{}", escapedLine);
                } else {
                    LOGGER.info("{}", escapedLine);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error reading tsc output", ex);
        }
    }

    private void createDir(Path path) {
        try {
            createRegistryFolder(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create directory " + path, ex);
        }
    }

    /**
     * Creates the registry folder, tolerating another party mutating the same tree.
     * <p>
     * {@link Files#createDirectories} is not atomic: it calls {@code mkdir}, and when that reports the
     * directory already exists it re-checks with {@link Files#isDirectory}. That check returns
     * {@code false} on <em>any</em> I/O error - including the path being removed in between - so a
     * concurrent delete of an ancestor turns "the directory is already there" into a fatal
     * {@link FileAlreadyExistsException}. That killed whole application contexts in CI, because this
     * runs from the {@code ApplicationReadyEvent} listener and any exception fails the startup.
     * <p>
     * Retrying is what makes this correct rather than merely quieter: a genuinely broken path (a
     * regular file where the folder belongs) still fails every attempt and is reported, while a
     * transient overlap resolves on the next one.
     *
     * @param path the registry folder
     * @throws IOException if the folder still cannot be created after the retries
     */
    static void createRegistryFolder(Path path) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= CREATE_FOLDER_ATTEMPTS; attempt++) {
            try {
                Files.createDirectories(path);
                return;
            } catch (FileAlreadyExistsException ex) {
                if (Files.isDirectory(path)) {
                    return; // it exists after all - another party won the race, which is fine
                }
                lastFailure = ex;
                LOGGER.debug("Attempt [{}] to create [{}] lost a race with a concurrent change; retrying", attempt, path, ex);
            }
        }
        throw lastFailure;
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying tsc watch service...");
        if (tscProcess != null) {
            if (tscProcess.isAlive()) {
                LOGGER.debug("Forcibly destroying tsc watch process {}...", tscProcess);
                tscProcess.destroyForcibly();
            }

            tscProcess = null;
        }

        if (null != executor) {
            LOGGER.debug("Shutting down the executor [{}]...", executor);
            executor.shutdownNow();

            executor = null;
        }

        LOGGER.info("Destroy completed!");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        start();
    }
}
