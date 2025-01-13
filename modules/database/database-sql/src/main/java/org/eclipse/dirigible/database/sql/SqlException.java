/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

/**
 * The Sql Exception.
 */
public class SqlException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 4878658205810743068L;

    /**
     * Instantiates a new sql exception.
     */
    public SqlException() {
        super();
    }

    /**
     * Instantiates a new sql exception.
     *
     * @param message the message
     * @param cause the cause
     * @param enableSuppression the enable suppression
     * @param writableStackTrace the writable stack trace
     */
    public SqlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Instantiates a new sql exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public SqlException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new sql exception.
     *
     * @param message the message
     */
    public SqlException(String message) {
        super(message);
    }

    /**
     * Instantiates a new sql exception.
     *
     * @param cause the cause
     */
    public SqlException(Throwable cause) {
        super(cause);
    }

}
