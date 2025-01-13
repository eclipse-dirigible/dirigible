/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.terminal.endpoint.TerminalWebsocketHandler.ProcessRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * The Class TerminalWebsocketConfig.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "terminal.enabled", havingValue = "true")
public class TerminalWebsocketConfig implements WebSocketConfigurer {

    /** The Constant TERMINAL_PREFIX. */
    private static final String TERMINAL_PREFIX = "[ws:terminal] ";

    // /** The Constant FEATURE_TERMINAL_IS_DISABLED_IN_THIS_MODE. */
    // private static final String FEATURE_TERMINAL_IS_DISABLED_IN_THIS_MODE = "Feature 'Terminal' is
    // disabled in this mode.";

    /** The Constant PERMISSIONS_FAILED. */
    private static final String PERMISSIONS_FAILED = "Failed to set permissions on file";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketConfig.class);

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

    /** The started. */
    static volatile boolean started = false;

    /**
     * Run TTYD.
     */
    public synchronized static void runTTYD() {
        if (!started) {
            // if (Configuration.isAnonymousModeEnabled()) {
            // if (logger.isWarnEnabled()) {logger.warn(TERMINAL_PREFIX +
            // FEATURE_TERMINAL_IS_DISABLED_IN_THIS_MODE);}
            // return;
            // }
            try {
                String command = "";
                String os = System.getProperty("os.name")
                                  .toLowerCase();
                if ((os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0)) {
                    command = "bash -c ./ttyd.sh";
                    File ttydShell = new File("./ttyd.sh");
                    if (!ttydShell.exists()) {
                        // ttyd binary should be placed in advance to $CATALINA_HOME/bin

                        createShellScript(ttydShell, "./ttyd -p 9000 -W bash");
                        if (ttydShell.setExecutable(true)) {
                            File ttydExecutable = new File("./ttyd");
                            createExecutable(TerminalWebsocketConfig.class.getResourceAsStream("/ttyd_linux.x86_64_1.6.0"), ttydExecutable);
                            if (!ttydExecutable.setExecutable(true)) {
                                if (logger.isWarnEnabled()) {
                                    logger.warn(TERMINAL_PREFIX + PERMISSIONS_FAILED);
                                }
                            }
                        } else {
                            if (logger.isWarnEnabled()) {
                                logger.warn(TERMINAL_PREFIX + PERMISSIONS_FAILED);
                            }
                        }
                    }
                } else if (os.indexOf("mac") >= 0) {
                    command = "bash -c ./ttyd.sh";
                    File ttydShell = new File("./ttyd.sh");
                    if (!ttydShell.exists()) {
                        // ttyd should be pre-installed with: brew install ttyd
                        // ProcessRunnable processRunnable = new ProcessRunnable("brew install ttyd");
                        // new Thread(processRunnable).start();
                        // processRunnable.getProcess().waitFor();

                        createShellScript(ttydShell, "ttyd -p 9000 -W bash");
                        ttydShell.setExecutable(true);
                    }
                } else if (os.indexOf("win") >= 0) {
                    logger.error("Windows is not yet supported");
                } else {
                    logger.error("Unknown OS: " + os);
                }

                ProcessRunnable processRunnable = new ProcessRunnable(command);
                new Thread(processRunnable).start();

            } catch (Exception e) {
                logger.error(TERMINAL_PREFIX + e.getMessage(), e);
            }
            started = true;
        }
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
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(command, fos, Charset.defaultCharset());
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
        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.copy(in, fos);
        }
    }

}
