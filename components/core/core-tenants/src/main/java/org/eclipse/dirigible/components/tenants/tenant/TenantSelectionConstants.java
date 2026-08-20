/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

/**
 * What the tenant selection of a user is stored under.
 *
 * <p>
 * The selection is written by the identity provider side (which validates it against the user's
 * groups) and read here, when the tenant scope of a request is opened - hence a shared constant
 * rather than a literal on each side.
 */
public final class TenantSelectionConstants {

    /** The HTTP session attribute carrying the id of the tenant the user selected. */
    public static final String SELECTED_TENANT_ID_SESSION_ATTRIBUTE = "dirigible-selected-tenant-id";

    private TenantSelectionConstants() {}
}
