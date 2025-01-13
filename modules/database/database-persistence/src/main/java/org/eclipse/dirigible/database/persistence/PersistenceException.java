/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence;

/**
 * The Persistence Exception.
 */
public class PersistenceException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5051153858321560002L;

    /**
     * Instantiates a new persistence exception.
     */
    public PersistenceException() {
        super();
    }

    /**
     * Instantiates a new persistence exception.
     *
     * @param message the message
     * @param cause the cause
     * @param enableSuppression the enable suppression
     * @param writableStackTrace the writable stack trace
     */
    public PersistenceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Instantiates a new persistence exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new persistence exception.
     *
     * @param message the message
     */
    public PersistenceException(String message) {
        super(message);
    }

    /**
     * Instantiates a new persistence exception.
     *
     * @param cause the cause
     */
    public PersistenceException(Throwable cause) {
        super(cause);
    }

}
