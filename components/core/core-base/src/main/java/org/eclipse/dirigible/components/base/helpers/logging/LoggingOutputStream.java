/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.helpers.logging;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * The Class LoggingOutputStream.
 */
public class LoggingOutputStream extends OutputStream {

    /** The baos. */
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);

    /** The logger. */
    private final Logger logger;

    /** The level. */
    private final LogLevel level;

    /**
     * The Enum LogLevel.
     */
    public enum LogLevel {

        /** The trace. */
        TRACE,
        /** The debug. */
        DEBUG,
        /** The info. */
        INFO,
        /** The warn. */
        WARN,
        /** The error. */
        ERROR,
    }

    /**
     * Instantiates a new logging output stream.
     *
     * @param logger the logger
     * @param level the level
     */
    public LoggingOutputStream(Logger logger, LogLevel level) {
        this.logger = logger;
        this.level = level;
    }

    /**
     * Write.
     *
     * @param b the b
     */
    @Override
    public void write(int b) {
        if (b == '\n') {
            String line = baos.toString();
            baos.reset();

            switch (level) {
                case TRACE:
                    logger.trace(line);
                    break;
                case DEBUG:
                    logger.debug(line);
                    break;
                case ERROR:
                    logger.error(line);
                    break;
                case INFO:
                    logger.info(line);
                    break;
                case WARN:
                    logger.warn(line);
                    break;
            }
        } else {
            baos.write(b);
        }
    }

}
