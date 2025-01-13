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
 * The Class RepositoryImportException.
 */
public class RepositoryImportException extends RepositoryException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -163847774919514248L;

    /**
     * Instantiates a new repository import exception.
     */
    public RepositoryImportException() {
        super();
    }

    /**
     * Instantiates a new repository import exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public RepositoryImportException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new repository import exception.
     *
     * @param message the message
     */
    public RepositoryImportException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository import exception.
     *
     * @param cause the cause
     */
    public RepositoryImportException(Throwable cause) {
        super(cause);
    }

}
