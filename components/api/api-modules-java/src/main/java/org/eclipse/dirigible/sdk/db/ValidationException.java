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
 * Thrown by client-side domain logic — a generated repository's declarative {@code checks:} gate, a
 * capacity guard, or a hand-written validation — to signal that a well-formed request violates a
 * business rule. The Java controller runtime maps it to HTTP {@code 400 Bad Request} carrying the
 * message, so a user-fixable validation surfaces as a client error instead of an opaque
 * {@code 500}.
 *
 * <p>
 * Throwing it from the repository keeps the persistence layer free of any web dependency: the
 * HTTP-status mapping lives once in the controller dispatcher, not in every controller. Raised on a
 * non-HTTP path (e.g. a BPMN service task), it simply fails that unit of work with its message, the
 * same as any other unchecked exception.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a validation exception.
     *
     * @param message the user-facing reason the request was rejected
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates a validation exception with an underlying cause.
     *
     * @param message the user-facing reason the request was rejected
     * @param cause the underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
