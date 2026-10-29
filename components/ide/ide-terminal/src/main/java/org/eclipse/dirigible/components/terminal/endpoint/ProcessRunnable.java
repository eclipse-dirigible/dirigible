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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import static org.eclipse.dirigible.components.terminal.endpoint.TerminalWebsocketConfig.TERMINAL_PREFIX;

class ProcessRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRunnable.class);

    /** The command line, split on whitespace into the program and its arguments. */
    private final String command;

    /** The variables added to the environment the command is started with. */
    private final Map<String, String> environment;

    /** The process. */
    private volatile Process process;

    /**
     * Instantiates a new process runnable.
     *
     * @param command the command
     * @param environment the variables added to the environment the command is started with
     */
    ProcessRunnable(String command, Map<String, String> environment) {
        this.command = command;
        this.environment = Map.copyOf(environment);
    }

    /**
     * Gets the process.
     *
     * @return the process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Run.
     */
    @Override
    public void run() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(this.command.split("\\s+"));
            processBuilder.environment()
                          .putAll(this.environment);
            this.process = processBuilder.start();

            pipeToLog("dirigible-ttyd-stdout-reader", process.getInputStream());
            pipeToLog("dirigible-ttyd-stderr-reader", process.getErrorStream());
        } catch (IOException e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
        }
    }

    /**
     * Drains a process stream to the log on a dedicated daemon thread, so that a running ttyd never
     * keeps the JVM alive after the application context is gone (e.g. on a failed startup).
     *
     * @param threadName the reader thread name
     * @param stream the process stream to drain
     */
    private static void pipeToLog(String threadName, InputStream stream) {
        Thread reader = new Thread(() -> {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = input.readLine()) != null) {
                    logger.info(TERMINAL_PREFIX, line);
                }
            } catch (IOException e) {
                logger.error(TERMINAL_PREFIX, e.getMessage(), e);
            }
        }, threadName);
        reader.setDaemon(true);
        reader.start();
    }

    /**
     * Destroys the ttyd process if it is still running. Closing its streams lets the reader threads
     * finish.
     */
    void destroy() {
        Process current = this.process;
        if (current != null && current.isAlive()) {
            logger.info("Destroying ttyd process [{}]", current.pid());
            current.destroy();
        }
    }

}
