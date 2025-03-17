/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.terminal.endpoint.TerminalWebsocketHandler.ProcessRunnable;
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

/**
 * The Class TerminalWebsocketConfig.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "terminal.enabled", havingValue = "true")
public class TerminalWebsocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketConfig.class);

    private static final String TERMINAL_PREFIX = "[ws:terminal] {}";
    private static final String UNIX_FILE = "ttyd.sh";
    private static volatile boolean started = false;

    static {
        runTTYD();
    }

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
        return new TerminalWebsocketHandler();
    }

    private synchronized static void runTTYD() {
        if (started) {
            logger.info("TTYD is started");
            return;
        }
        startTTYD();
    }

    private static void startTTYD() {
        try {
            if (SystemUtils.IS_OS_UNIX) {
                File unixFile = createUnixFile();

                String command = "./" + unixFile.getName();
                ProcessRunnable processRunnable = new ProcessRunnable(command);

                new Thread(processRunnable).start();

                started = true;
            } else {
                logger.warn("OS [{}] is not supported", System.getProperty("os.name"));
            }
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
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
     * Creates the shell script.
     *
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void createShellScriptFile(File file) throws FileNotFoundException, IOException {
        String command = "ttyd -p 9000 --writable sh";
        logger.info("Creating file [{}] with content [{}]", file, command);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(command, fos, StandardCharsets.UTF_8);
        }
        boolean completed = file.setExecutable(true);
        logger.info("File [{}] set as executable [{}]", completed);
    }

}
