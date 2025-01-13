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
 * The {@link RepositoryException} is thrown in situations when a user is trying to use any of the
 * repository API in a way that is invalid or not expected.<br>
 * One such example is when passing <code>null</code> to a method when it does not expect it. This
 * would likely result in a {@link IllegalArgumentException} or a {@link NullPointerException}
 * depending on the implementation's code conventions. The first exception type is preferred though.
 */
public class RepositoryException extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new repository exception.
     */
    public RepositoryException() {
        super();
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param message the message
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param ex the ex
     */
    public RepositoryException(Throwable ex) {
        super(ex);
    }

    /**
     * Instantiates a new repository exception.
     *
     * @param message the message
     * @param ex the ex
     */
    public RepositoryException(String message, Throwable ex) {
        super(message, ex);
    }

}
