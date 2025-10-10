/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.config;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    // Default tenant to be used when no tenant is explicitly set (e.g., for common tables/metadata)
    private static final String DEFAULT_TENANT = "default-tenant";

    /** The tenant context. */
    private final TenantContext tenantContext;

    @Autowired
    public CurrentTenantIdentifierResolverImpl(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        return (this.tenantContext.getCurrentTenant() != null) ? this.tenantContext.getCurrentTenant()
                                                                                   .getId()
                : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
