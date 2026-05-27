/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drains a child process's stdout/stderr to the platform logger on a daemon thread. Without this
 * the child OS pipes can fill and block the child process indefinitely.
 */
final class ProcessLogPump {

    private static final AtomicLong THREAD_COUNTER = new AtomicLong();

    private ProcessLogPump() {}

    static void start(String appName, Process process) {
        String logBase = "org.eclipse.dirigible.nativeapps." + appName;
        Thread out = new Thread(() -> drain(LoggerFactory.getLogger(logBase + ".stdout"), process.getInputStream(), false),
                "native-app-stdout-" + appName + "-" + THREAD_COUNTER.incrementAndGet());
        Thread err = new Thread(() -> drain(LoggerFactory.getLogger(logBase + ".stderr"), process.getErrorStream(), true),
                "native-app-stderr-" + appName + "-" + THREAD_COUNTER.incrementAndGet());
        out.setDaemon(true);
        err.setDaemon(true);
        out.start();
        err.start();
    }

    private static void drain(Logger logger, InputStream stream, boolean asWarn) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (asWarn) {
                    logger.warn(line);
                } else {
                    logger.info(line);
                }
            }
        } catch (IOException ex) {
            logger.debug("Native-app log pump stopped: {}", ex.getMessage());
        }
    }
}
