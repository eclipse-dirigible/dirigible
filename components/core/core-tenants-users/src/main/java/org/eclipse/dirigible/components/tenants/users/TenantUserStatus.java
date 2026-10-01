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
 * Where a tenant user stands, as the external provisioning system last reported it.
 *
 * <p>
 * The replica stores what the snapshot says and nothing this application derives itself: whether
 * the person has entered the tenant is {@link TenantUser#getLastSignInAt()}, not a status.
 */
public enum TenantUserStatus {

    /** A request for the person is being worked on, and no account outcome is known yet. */
    PENDING,

    /** The person's account was created for this membership; they still have to sign in once. */
    INVITED,

    /** The person's existing account was given the membership. */
    ASSIGNED,

    /** The membership could not be established; the user's last error says why. */
    FAILED,

    /**
     * The person was removed from the tenant. The row is kept as a hidden tombstone, so a late snapshot
     * of an older revision cannot bring them back.
     */
    REMOVED
}
