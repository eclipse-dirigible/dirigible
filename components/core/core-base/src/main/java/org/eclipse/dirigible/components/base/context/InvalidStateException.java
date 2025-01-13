/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.context;

/**
 * The InvalidStateException.
 */
public class InvalidStateException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5039208772641246649L;

    /**
     * Instantiates a new invalid state exception.
     */
    public InvalidStateException() {
        super();
    }

    /**
     * Instantiates a new invalid state exception.
     *
     * @param message the message
     * @param cause the cause
     * @param enableSuppression the enable suppression
     * @param writableStackTrace the writable stack trace
     */
    public InvalidStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Instantiates a new invalid state exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new invalid state exception.
     *
     * @param message the message
     */
    public InvalidStateException(String message) {
        super(message);
    }

    /**
     * Instantiates a new invalid state exception.
     *
     * @param cause the cause
     */
    public InvalidStateException(Throwable cause) {
        super(cause);
    }

}
