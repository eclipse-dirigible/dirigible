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
        /** The tenant exists for the user, but this instance has not provisioned it yet. */
        NOT_PROVISIONED_HERE,
        /** The request is not an interactive session that could hold a selection. */
        NOT_AN_INTERACTIVE_SESSION
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
