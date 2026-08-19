/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.core;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.sdk.component.Beans;

/**
 * The tenant the current execution belongs to. Requests carry it from the host they arrived on, and
 * the platform re-establishes it around a listener dispatch or a scheduled job, so client code can
 * read it anywhere it can read {@link org.eclipse.dirigible.sdk.security.User} — including from a
 * message handler, where there is no request at all.
 * <p>
 * Use it to stamp outward-facing messages and audit records with who the sender is. Do NOT use it
 * to scope queries: the platform already routes a tenant's data access to that tenant's schema, so
 * a hand-rolled tenant filter is at best redundant and at worst a second, divergent rule.
 * <p>
 * Outside any tenant scope the id resolves to the default tenant, which is what a single-tenant
 * deployment runs as.
 */
public final class Tenant {

    private Tenant() {}

    /**
     * The current tenant's id.
     *
     * @return the tenant id, never {@code null}
     */
    public static String getId() {
        return current().getId();
    }

    /**
     * The current tenant's display name.
     *
     * @return the tenant name, never {@code null}
     */
    public static String getName() {
        return current().getName();
    }

    private static org.eclipse.dirigible.components.base.tenant.Tenant current() {
        TenantContext context = Beans.get(TenantContext.class);
        return context.isInitialized() ? context.getCurrentTenant() : Beans.get(org.eclipse.dirigible.components.base.tenant.Tenant.class);
    }
}
