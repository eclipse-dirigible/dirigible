/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import org.springframework.http.HttpStatus;

/**
 * A tenant a user asked for cannot be entered.
 *
 * <p>
 * Carries the reason rather than an HTTP status, because the same refusal is answered differently
 * depending on who asked - a REST client gets a status, the selection filter gets to redirect.
 */
public class TenantSelectionException extends RuntimeException {

    /** The serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Why a tenant cannot be entered.
     */
    public enum Reason {
        /** The groups of the user do not grant the tenant in this application. */
        NOT_A_MEMBER,
        /** The tenant is registered here, but this instance has not finished provisioning it. */
        NOT_PROVISIONED_HERE,
        /**
         * The tenant is not registered in this instance at all, so nothing here is preparing it and the
         * refusal will not resolve itself by waiting.
         */
        UNKNOWN_HERE,
        /** The request is not an interactive session that could hold a selection. */
        NOT_AN_INTERACTIVE_SESSION;

        /**
         * The status a programmatic caller is answered with. {@code NOT_PROVISIONED_HERE} and
         * {@code UNKNOWN_HERE} share one deliberately: the status is the contract, the reason is what tells
         * the two apart - one ends by waiting and the other never does.
         *
         * @return the HTTP status
         */
        public HttpStatus httpStatus() {
            return switch (this) {
                case NOT_A_MEMBER -> HttpStatus.FORBIDDEN;
                case NOT_PROVISIONED_HERE, UNKNOWN_HERE -> HttpStatus.CONFLICT;
                case NOT_AN_INTERACTIVE_SESSION -> HttpStatus.UNAUTHORIZED;
            };
        }
    }

    private final Reason reason;

    private final String tenantId;

    /**
     * Instantiates a new tenant selection exception.
     *
     * @param reason the reason
     * @param tenantId the tenant that was asked for
     * @param message the message
     */
    public TenantSelectionException(Reason reason, String tenantId, String message) {
        super(message);
        this.reason = reason;
        this.tenantId = tenantId;
    }

    /**
     * Gets the reason.
     *
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Gets the tenant that was asked for.
     *
     * @return the tenant id
     */
    public String getTenantId() {
        return tenantId;
    }
}
