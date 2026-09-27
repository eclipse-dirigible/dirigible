/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.roles;

import java.util.List;

/**
 * The roles an application grants inside a tenant - under {@code TOKEN_GROUPS} the third segment of
 * the identity-provider group {@code <tenantId>.<appId>.<role>}, and the authority
 * {@code ROLE_<role>} of a caller who entered that tenant. Tenant users management grants exactly
 * these; {@link #OWNER} manages the tenant's users.
 */
public final class ApplicationRoles {

    /** The owner of a tenant: manages its users. */
    public static final String OWNER = "Owner";

    /** A member of a tenant. */
    public static final String USER = "User";

    /** Every application role, in the order a page offers them. */
    public static final List<String> ALL = List.of(OWNER, USER);

    private ApplicationRoles() {}
}
