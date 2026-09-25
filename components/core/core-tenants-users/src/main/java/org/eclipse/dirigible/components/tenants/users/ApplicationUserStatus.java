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

/**
 * Where a tenant's application user stands.
 *
 * <p>
 * {@link #PENDING} and {@link #ACTIVE} are this application's own knowledge - the request was
 * published, the person entered the tenant. {@link #INVITED}, {@link #ASSIGNED} and {@link #FAILED}
 * are recorded by the external provisioning system through the users callback. A callback never
 * lowers a status: see {@link #rank()}.
 */
public enum ApplicationUserStatus {

    /** The request was published and nothing has been heard yet. */
    PENDING(0),

    /** The request failed before any role was granted. */
    FAILED(0),

    /** The account existed and the role is granted. */
    ASSIGNED(1),

    /** The account was created for this request; the person still has to set a password. */
    INVITED(2),

    /** The person has entered the tenant at least once. */
    ACTIVE(3);

    /** The precedence of the status. */
    private final int rank;

    /**
     * Instantiates a status.
     *
     * @param rank the precedence
     */
    ApplicationUserStatus(int rank) {
        this.rank = rank;
    }

    /**
     * The precedence a callback respects: a status is replaced only by one of higher rank.
     *
     * @return the rank
     */
    int rank() {
        return rank;
    }
}
