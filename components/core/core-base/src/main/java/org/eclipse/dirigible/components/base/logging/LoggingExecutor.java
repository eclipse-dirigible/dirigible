/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.logging;

import javax.sql.DataSource;
import org.eclipse.dirigible.components.base.callable.CallableNoResultAndException;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.callable.CallableResultAndNoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExecutor.class);

    public static <T extends Throwable> void executeNoResultWithException(DataSource dataSource, CallableNoResultAndException<T> callable)
            throws Throwable {
        try {
            callable.call();
        } catch (Throwable ex) {
            LOGGER.error("Failed to execute a code [{}] for data source [{}] with message [{}]", callable, dataSource, ex.getMessage());
            throw ex;
        }
    }

    public static <R, T extends Throwable> R executeWithException(DataSource dataSource, CallableResultAndException<R, T> callable)
            throws Throwable {
        try {
            return callable.call();
        } catch (Throwable ex) {
            LOGGER.error("Failed to execute a code [{}] for data source [{}] with message [{}]", callable, dataSource, ex.getMessage());
            throw ex;
        }
    }

    public static <R> R executeWithoutException(CallableResultAndNoException<R> callable) throws RuntimeException {
        try {
            return callable.call();
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to execute a code [{}] with message [{}]", callable, ex.getMessage());
            throw ex;
        }
    }
}
