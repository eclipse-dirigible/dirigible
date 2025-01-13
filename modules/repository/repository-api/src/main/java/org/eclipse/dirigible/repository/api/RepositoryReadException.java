/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

/**
 * The Repository Read Exception.
 */
public class RepositoryReadException extends RepositoryException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -163847774919514248L;

    /**
     * Instantiates a new repository read exception.
     */
    public RepositoryReadException() {
        super();
    }

    /**
     * Instantiates a new repository read exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public RepositoryReadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new repository read exception.
     *
     * @param message the message
     */
    public RepositoryReadException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository read exception.
     *
     * @param cause the cause
     */
    public RepositoryReadException(Throwable cause) {
        super(cause);
    }

}
