/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import org.springframework.http.HttpStatus;

/**
 * A refusal of the tenant users endpoints, with a machine-readable reason the page translates.
 */
class TenantUsersException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The status. */
    private final HttpStatus status;

    /** The reason. */
    private final String reason;

    /**
     * Instantiates a refusal.
     *
     * @param status the status
     * @param reason the reason
     * @param message the explanation
     */
    TenantUsersException(HttpStatus status, String reason, String message) {
        super(message);
        this.status = status;
        this.reason = reason;
    }

    HttpStatus status() {
        return status;
    }

    String reason() {
        return reason;
    }
}
