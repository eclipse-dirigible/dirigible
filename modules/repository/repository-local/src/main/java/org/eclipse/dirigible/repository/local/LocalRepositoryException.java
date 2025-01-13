/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import org.eclipse.dirigible.repository.api.RepositoryException;

/**
 * Main exception for the DB repository implementation.
 */
public class LocalRepositoryException extends RepositoryException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 116149128529374300L;

    /**
     * Instantiates a new local repository exception.
     */
    public LocalRepositoryException() {
        super();
    }

    /**
     * Instantiates a new local repository exception.
     *
     * @param arg0 the arg 0
     * @param arg1 the arg 1
     */
    public LocalRepositoryException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Instantiates a new local repository exception.
     *
     * @param arg0 the arg 0
     */
    public LocalRepositoryException(String arg0) {
        super(arg0);
    }

    /**
     * Instantiates a new local repository exception.
     *
     * @param arg0 the arg 0
     */
    public LocalRepositoryException(Throwable arg0) {
        super(arg0);
    }

}
