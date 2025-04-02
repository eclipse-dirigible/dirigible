/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
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
import java.io.InputStreamReader;

import static org.eclipse.dirigible.components.terminal.endpoint.TerminalWebsocketConfig.TERMINAL_PREFIX;

class ProcessRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProcessRunnable.class);

    /** The command. */
    private final String command;

    /** The process. */
    private Process process;

    /**
     * Instantiates a new process runnable.
     *
     * @param command the command
     */
    ProcessRunnable(String command) {
        this.command = command;
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
            this.process = Runtime.getRuntime()
                                  .exec(this.command);

            Thread reader = new Thread(() -> {
                try {
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;

                        while ((line = input.readLine()) != null) {
                            logger.info(TERMINAL_PREFIX, line);
                        }
                    }
                } catch (IOException e) {
                    logger.error(TERMINAL_PREFIX, e.getMessage(), e);
                }
            });
            reader.start();

            Thread error = new Thread(() -> {
                try {
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;

                        while ((line = input.readLine()) != null) {
                            logger.info(TERMINAL_PREFIX, line);
                        }
                    }
                } catch (IOException e) {
                    logger.error(TERMINAL_PREFIX, e.getMessage(), e);
                }
            });
            error.start();

        } catch (IOException e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
        }

    }

}
