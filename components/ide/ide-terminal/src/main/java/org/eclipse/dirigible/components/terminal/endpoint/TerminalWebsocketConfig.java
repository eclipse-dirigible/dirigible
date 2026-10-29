/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The Class TerminalWebsocketConfig.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "terminal.enabled", havingValue = "true")
public class TerminalWebsocketConfig implements WebSocketConfigurer {

    static final String TERMINAL_PREFIX = "[ws:terminal] {}";
    static final String TTYD_HOST = "127.0.0.1";
    static final int TTYD_PORT = 9000;
    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketConfig.class);
    private static final String UNIX_FILE = "ttyd.sh";
    private static final String CREDENTIAL_VARIABLE = "DIRIGIBLE_TERMINAL_CREDENTIAL";
    /** One credential per process, so a ttyd still closing for an earlier context accepts it too. */
    private static final TtydCredential CREDENTIAL = TtydCredential.generate();

    /** The ttyd process, started on context init and destroyed when the context closes. */
    private ProcessRunnable ttydProcess;

    /**
     * Register web socket handlers.
     *
     * @param registry the registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getConsoleWebsocketHandler(), BaseEndpoint.PREFIX_ENDPOINT_WEBSOCKETS + "ide/terminal");
    }

    /**
     * Gets the data transfer websocket handler.
     *
     * @return the data transfer websocket handler
     */
    @Bean
    public WebSocketHandler getConsoleWebsocketHandler() {
        return new TerminalWebsocketHandler(CREDENTIAL.token());
    }

    /**
     * Starts ttyd when the context is initialized.
     */
    @PostConstruct
    void startTTYD() {
        try {
            if (!SystemUtils.IS_OS_UNIX) {
                logger.warn("OS [{}] is not supported", System.getProperty("os.name"));
            }

            File unixFile = createUnixFile();

            String command = "./" + unixFile.getName();

            logger.info("Starting ttyd using command [{}] and file [{}]", command, unixFile);
            ProcessRunnable processRunnable = new ProcessRunnable(command, Map.of(CREDENTIAL_VARIABLE, CREDENTIAL.credential()));
            Thread ttydThread = new Thread(processRunnable, "dirigible-ttyd-launcher");
            ttydThread.setDaemon(true);
            ttydThread.start();

            this.ttydProcess = processRunnable;
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
        }
    }

    /**
     * Destroys ttyd when the context is closed, so its reader threads do not outlive the application.
     */
    @PreDestroy
    void stopTTYD() {
        if (ttydProcess != null) {
            ttydProcess.destroy();
        }
    }

    private static File createUnixFile() throws IOException {
        File ttydShellFile = new File("./" + UNIX_FILE);
        if (ttydShellFile.exists()) {
            boolean deleted = ttydShellFile.delete();
            logger.info("File [{}] deleted [{}]", ttydShellFile, deleted);
        }

        createShellScriptFile(ttydShellFile);

        return ttydShellFile;
    }

    /**
     * Creates the shell script. The backend listens on the loopback interface only, where the terminal
     * websocket connects to it, and takes its credential from the environment the script is started
     * with, so it is written neither to the file nor to the log.
     *
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void createShellScriptFile(File file) throws FileNotFoundException, IOException {
        String command = """
                #!/bin/sh
                ttyd -i %s -p %d -c "$%s" --writable sh
                """.formatted(TTYD_HOST, TTYD_PORT, CREDENTIAL_VARIABLE);
        logger.info("Creating file [{}] with content [{}]", file, command);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(command, fos, StandardCharsets.UTF_8);
        }
        boolean completed = file.setExecutable(true);
        logger.info("File [{}] set as executable [{}]", file, completed);
    }

}
