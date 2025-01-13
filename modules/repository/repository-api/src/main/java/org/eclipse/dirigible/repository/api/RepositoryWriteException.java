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
 * The Repository Write Exception.
 */
public class RepositoryWriteException extends RepositoryException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -163847774919514248L;

    /**
     * Instantiates a new repository write exception.
     */
    public RepositoryWriteException() {
        super();
    }

    /**
     * Instantiates a new repository write exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public RepositoryWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new repository write exception.
     *
     * @param message the message
     */
    public RepositoryWriteException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository write exception.
     *
     * @param cause the cause
     */
    public RepositoryWriteException(Throwable cause) {
        super(cause);
    }

}
