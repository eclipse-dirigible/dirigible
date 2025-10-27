package org.eclipse.dirigible.components.engine.typescript.transpilation;

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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Order(ApplicationListenersOrder.ApplicationReadyEventListeners.TYPE_SCRIPT_TRANSPILATION_SERVICE)
@Component
@ConditionalOnExpression("'${DIRIGIBLE_TSC_WATCH_SERVICE_ENABLED:true}'.toLowerCase() != 'false'")
class TscWatcherService implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TscWatcherService.class);

    // Matches terminal control characters and ANSI sequences that can clear or move the console cursor:
    // \r - carriage return (rewrites current line)
    // \u0007 - bell character (beep)
    // \u001B[2J - clear screen
    // \u001B[H - move cursor to home position (top-left)
    // These are stripped from tsc output to prevent log lines from disappearing or refreshing.
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\r\\u0007\\u001B\\[2J\\u001B\\[H]");

    private static final String TS_CONFIG_CONTENT = """
            {
                "compilerOptions": {
                    "module": "ESNext",
                    "target": "ES6",
                    "moduleResolution": "Node",
                    "baseUrl": "./",
                    "lib": [
                        "ESNext",
                        "DOM"
                    ],
                    "paths": {
                        "sdk/*": [
                            "./modules/src/*"
                        ],
                        "/*": [
                            "./*"
                        ]
                    },
                    "types": [
                        "./modules/types"
                    ]
                },
                "exclude": ["modules"]
            }
            """;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final IRepository repository;
    private Process tscProcess;

    TscWatcherService(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks every 30 seconds if the tsc process is alive. If not, restarts it.
     */
    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void monitorTscProcess() {
        if (tscProcess == null || !tscProcess.isAlive()) {
            LOGGER.warn("tsc watch service is not initialized or it is not alive. Will start it again.");
            onApplicationEvent(null);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            createOrReplaceTsConfig();
            startTscWatch();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to start tsc watch service", ex);
        }
    }

    private void createOrReplaceTsConfig() {
        String registryFolderPath = getRegistryFolderPath();

        Path tsConfigPath = Paths.get(registryFolderPath, "tsconfig.json");

        LOGGER.info("Creating registry tsconfig.json file with path [{}] and content:\n{}", tsConfigPath, TS_CONFIG_CONTENT);
        try {
            Files.createDirectories(Paths.get(registryFolderPath));
            Files.writeString(tsConfigPath, TS_CONFIG_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create registry tsconfig.json with path [" + tsConfigPath + "]", ex);
        }
    }

    private String getRegistryFolderPath() {
        return this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
    }

    private synchronized void startTscWatch() {
        if (tscProcess != null && tscProcess.isAlive()) {
            LOGGER.info("TSC watch process is already running and will not be retriggered. Process [{}]", tscProcess);
            return;
        }

        String registryFolderPath = getRegistryFolderPath();
        try {
            LOGGER.info("Starting tsc watch in dir [{}]...", registryFolderPath);

            ProcessBuilder processBuilder = new ProcessBuilder("tsc", "--watch", "--pretty", "false");
            processBuilder.directory(new File(registryFolderPath));
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

    @Override
    public void destroy() throws Exception {
        LOGGER.info("Destroying tsc watch service...");
        if (tscProcess != null && tscProcess.isAlive()) {
            LOGGER.info("Stopping tsc watch process...");
            tscProcess.destroy();
        }
        executor.shutdownNow();
    }
}
