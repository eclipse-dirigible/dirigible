/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

/**
 * A test-only stand-in for the client SDK's validation exception. It carries the same fully
 * qualified name because that name IS the contract this module matches on: it cannot depend on
 * {@code api-modules-java} (the cycle is documented on {@code ClientValidationFailure}), so the
 * real class is only ever seen through the client class loader at run time.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a validation exception.
     *
     * @param message the user-facing reason
     */
    public ValidationException(String message) {
        super(message);
    }
}
