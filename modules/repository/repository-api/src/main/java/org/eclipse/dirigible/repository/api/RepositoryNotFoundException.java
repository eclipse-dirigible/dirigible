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
 * The Class RepositoryNotFoundException.
 */
public class RepositoryNotFoundException extends RepositoryReadException {

    private static final long serialVersionUID = 1912791323774243568L;

    /**
     * Instantiates a new repository not found exception.
     */
    public RepositoryNotFoundException() {
        super();
    }

    /**
     * Instantiates a new repository not found exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public RepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new repository not found exception.
     *
     * @param message the message
     */
    public RepositoryNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository not found exception.
     *
     * @param cause the cause
     */
    public RepositoryNotFoundException(Throwable cause) {
        super(cause);
    }

}
