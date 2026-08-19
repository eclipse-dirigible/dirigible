/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

/**
 * A failed {@code cognito-idp} API call. Carries the service error type (e.g.
 * {@code NotAuthorizedException}) so the caller can map it onto a normalized outcome - the raw
 * message stays a server-side diagnostic.
 */
class CognitoIdpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The service error type, or {@code null} when the call failed before a service answer. */
    private final String errorType;

    CognitoIdpException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    CognitoIdpException(String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * The service error type.
     *
     * @return the error type, or {@code null} when the call failed without a service answer
     */
    String getErrorType() {
        return errorType;
    }
}
