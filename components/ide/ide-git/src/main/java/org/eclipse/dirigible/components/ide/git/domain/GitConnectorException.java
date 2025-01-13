/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

/**
 * The GitConnectorException.
 */
public class GitConnectorException extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3164412135969838078L;

    /**
     * Instantiates a new git connector exception.
     */
    public GitConnectorException() {
        super();
    }

    /**
     * Instantiates a new git connector exception.
     *
     * @param message the message
     * @param cause the cause
     * @param enableSuppression the enable suppression
     * @param writableStackTrace the writable stack trace
     */
    public GitConnectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Instantiates a new git connector exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public GitConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new git connector exception.
     *
     * @param message the message
     */
    public GitConnectorException(String message) {
        super(message);
    }

    /**
     * Instantiates a new git connector exception.
     *
     * @param cause the cause
     */
    public GitConnectorException(Throwable cause) {
        super(cause);
    }

}
