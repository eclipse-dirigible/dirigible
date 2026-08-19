/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

/**
 * A failed native login step. Carries a normalized {@link Outcome} - the only thing surfaced to the
 * client, so provider-raw messages (which risk user enumeration and defeat application-side i18n)
 * never leave the server.
 */
public class NativeLoginException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * The normalized login outcomes. Providers map their raw error codes onto these; clients route the
     * user (retry, reset password, confirm account, ...) based on them.
     */
    public enum Outcome {
        /** The request is malformed or names an unknown client registration. */
        INVALID_REQUEST,
        /** The credentials were refused (also covers unknown users, to avoid user enumeration). */
        INVALID_CREDENTIALS,
        /** The provider requires a password reset before the user can sign in. */
        PASSWORD_RESET_REQUIRED,
        /** The user account exists but has not been confirmed yet. */
        USER_NOT_CONFIRMED,
        /** The submitted challenge code did not match. */
        CODE_MISMATCH,
        /** The submitted challenge code expired. */
        CODE_EXPIRED,
        /** The submitted new password was refused by the provider's password policy. */
        INVALID_PASSWORD,
        /** The provider throttled the attempt. */
        TOO_MANY_ATTEMPTS,
        /** Any other authentication failure. */
        AUTHENTICATION_FAILED
    }

    /** The normalized outcome. */
    private final Outcome outcome;

    /**
     * Instantiates a new native login exception.
     *
     * @param outcome the normalized outcome
     * @param message the server-side diagnostic message (never surfaced to the client)
     */
    public NativeLoginException(Outcome outcome, String message) {
        super(message);
        this.outcome = outcome;
    }

    /**
     * Instantiates a new native login exception.
     *
     * @param outcome the normalized outcome
     * @param message the server-side diagnostic message (never surfaced to the client)
     * @param cause the cause
     */
    public NativeLoginException(Outcome outcome, String message, Throwable cause) {
        super(message, cause);
        this.outcome = outcome;
    }

    /**
     * Gets the normalized outcome.
     *
     * @return the outcome
     */
    public Outcome getOutcome() {
        return outcome;
    }
}
