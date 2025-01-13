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
 * The Repository Search Exception.
 */
public class RepositorySearchException extends RepositoryException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -163847774919514248L;

    /**
     * Instantiates a new repository search exception.
     */
    public RepositorySearchException() {
        super();
    }

    /**
     * Instantiates a new repository search exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public RepositorySearchException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new repository search exception.
     *
     * @param message the message
     */
    public RepositorySearchException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository search exception.
     *
     * @param cause the cause
     */
    public RepositorySearchException(Throwable cause) {
        super(cause);
    }

}
