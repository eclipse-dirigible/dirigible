/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tracing;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the tenant beans that {@link TaskStateService} depends on. The module does not pull in
 * core-tenants, so the context is given a mock tenant context that resolves to the default tenant.
 */
@TestConfiguration
public class TestConfig {

    /** The default tenant id used across the tracing tests. */
    static final String DEFAULT_TENANT_ID = "default-tenant";

    /**
     * Mock tenant context reporting an uninitialized context, so tenant resolution falls back to the
     * default tenant.
     *
     * @return the tenant context
     */
    @Bean
    TenantContext tenantContext() {
        TenantContext tenantContext = Mockito.mock(TenantContext.class);
        Mockito.when(tenantContext.isNotInitialized())
               .thenReturn(true);
        return tenantContext;
    }

    /**
     * Mock default tenant with a stable id.
     *
     * @return the default tenant
     */
    @Bean
    @DefaultTenant
    Tenant defaultTenant() {
        Tenant tenant = Mockito.mock(Tenant.class);
        Mockito.when(tenant.getId())
               .thenReturn(DEFAULT_TENANT_ID);
        return tenant;
    }
}
