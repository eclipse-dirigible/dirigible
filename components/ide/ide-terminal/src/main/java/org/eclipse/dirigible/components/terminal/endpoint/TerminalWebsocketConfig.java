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

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * The Class TerminalWebsocketConfig.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "terminal.enabled", havingValue = "true")
public class TerminalWebsocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketConfig.class);

    /** The Constant TERMINAL_PREFIX. */
    private static final String TERMINAL_PREFIX = "[ws:terminal] {}";
    private static final String UNIX_FILE = "ttyd.sh";
    /** The started. */
    static volatile boolean started = false;

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
            String command = null;
            if (SystemUtils.IS_OS_UNIX) {
                command = createUnixCommand();
            } else {
                logger.warn("OS [{}] is not supported", System.getProperty("os.name"));
            }

            if (null != command) {
                logger.info("Starting ttyd using command [{}]", command);
                ProcessRunnable processRunnable = new ProcessRunnable(command);
                new Thread(processRunnable).start();

                started = true;
            }
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
        }
    }

    private static String createUnixCommand() throws IOException {
        String command = "sh -c ./" + UNIX_FILE + " --writable";
        File ttydShellFile = new File("./" + UNIX_FILE);
        if (ttydShellFile.exists()) {
            boolean deleted = ttydShellFile.delete();
            logger.info("File [{}] deleted [{}]", ttydShellFile, deleted);
        }
        createShellScript(ttydShellFile, "./" + UNIX_FILE + " -p 9000 --writable sh");

        if (!ttydShellFile.setExecutable(true)) {
            logger.warn(TERMINAL_PREFIX, "Failed to set permissions on file");
            File ttydExecutable = new File("./" + UNIX_FILE + " --writable");
            createExecutable(TerminalWebsocketConfig.class.getResourceAsStream("/ttyd_linux.x86_64_1.6.0"), ttydExecutable);
        }
        return command;
    }

    /**
     * Creates the shell script.
     *
     * @param file the file
     * @param command the command
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void createShellScript(File file, String command) throws FileNotFoundException, IOException {
        logger.info("Creating file [{}] with content [{}]", file, command);
        file.setExecutable(true);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(command, fos, StandardCharsets.UTF_8);
        }
    }

    /**
     * Creates the executable.
     *
     * @param in the in
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void createExecutable(InputStream in, File file) throws FileNotFoundException, IOException {
        file.setExecutable(true);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(in, fos);
        }
    }

}
