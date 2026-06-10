/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.log;

import java.io.IOException;
import java.util.Arrays;
import org.eclipse.dirigible.components.api.log.LogFacade;

/**
 * Named application logger. Each level method ({@link #trace}, {@link #debug}, {@link #info},
 * {@link #warn}, {@link #error}) accepts an SLF4J-style template plus varargs — placeholders are
 * {@code {}} (not {@code {0}}), and a trailing {@link Throwable} is unpacked into the stack-trace
 * slot by SLF4J automatically.
 * <p>
 * Calls go through {@code LogFacade}, which prefixes the supplied name with {@code app.} so log
 * records share configuration and appenders with the rest of the platform.
 * {@link #setLevel(String)} adjusts the level at runtime via Logback; the {@code isXxxEnabled}
 * predicates let you guard expensive argument construction.
 *
 * <pre>
 * Logger log = Logging.getLogger("com.acme.orders");
 * if (log.isDebugEnabled()) {
 *     log.debug("payload received: {}", payload);
 * }
 * try { ... } catch (IOException ex) {
 *     log.error("failed to load order {}", orderId, ex);   // stack trace is captured
 * }
 * </pre>
 */
public final class Logger {

    private final String loggerName;

    Logger(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getName() {
        return loggerName;
    }

    public Logger setLevel(String level) {
        LogFacade.setLevel(loggerName, level);
        return this;
    }

    public boolean isTraceEnabled() {
        try {
            return LogFacade.isTraceEnabled(loggerName);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean isDebugEnabled() {
        try {
            return LogFacade.isDebugEnabled(loggerName);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean isInfoEnabled() {
        try {
            return LogFacade.isInfoEnabled(loggerName);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean isWarnEnabled() {
        try {
            return LogFacade.isWarnEnabled(loggerName);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean isErrorEnabled() {
        try {
            return LogFacade.isErrorEnabled(loggerName);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void trace(String message, Object... args) {
        log("TRACE", message, args);
    }

    public void debug(String message, Object... args) {
        log("DEBUG", message, args);
    }

    public void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public void warn(String message, Object... args) {
        log("WARN", message, args);
    }

    public void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    private void log(String level, String message, Object[] args) {
        org.slf4j.Logger backing = LogFacade.getLogger(loggerName);
        switch (level) {
            case "TRACE" -> {
                if (backing.isTraceEnabled()) {
                    backing.trace(message, normalize(args));
                }
            }
            case "DEBUG" -> {
                if (backing.isDebugEnabled()) {
                    backing.debug(message, normalize(args));
                }
            }
            case "INFO" -> {
                if (backing.isInfoEnabled()) {
                    backing.info(message, normalize(args));
                }
            }
            case "WARN" -> {
                if (backing.isWarnEnabled()) {
                    backing.warn(message, normalize(args));
                }
            }
            case "ERROR" -> {
                if (backing.isErrorEnabled()) {
                    backing.error(message, normalize(args));
                }
            }
            default -> throw new IllegalArgumentException("Unknown log level: " + level);
        }
    }

    private static Object[] normalize(Object[] args) {
        return args == null ? new Object[0] : Arrays.copyOf(args, args.length);
    }
}
