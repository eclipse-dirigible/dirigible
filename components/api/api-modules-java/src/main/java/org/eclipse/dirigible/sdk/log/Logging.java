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

/**
 * Entry point for obtaining named application loggers. Names are conventionally dot-separated
 * package paths ({@code com.acme.orders.fulfilment}) and are nested under the platform's
 * {@code app.} root, so the same configuration knobs that govern other application loggers also
 * govern these — set levels in {@code logback.xml}, override at runtime through
 * {@link Logger#setLevel(String)}.
 * <p>
 * Loggers returned here are independent value objects; cache them in a {@code static final} field
 * per class if performance matters.
 *
 * <pre>
 * private static final Logger LOG = Logging.getLogger("com.acme.orders");
 * LOG.info("processed order {}", orderId);
 * </pre>
 */
public final class Logging {

    private Logging() {}

    public static Logger getLogger(String loggerName) {
        return new Logger(loggerName);
    }
}
