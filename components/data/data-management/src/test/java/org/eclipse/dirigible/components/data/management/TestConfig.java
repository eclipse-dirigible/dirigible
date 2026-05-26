/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    TenantContext tenantContext() {
        TenantContext context = Mockito.mock(TenantContext.class);
        when(context.isInitialized()).thenReturn(false);
        when(context.isNotInitialized()).thenReturn(true);
        return context;
    }

    @Bean
    @DefaultTenant
    Tenant defaultTenant() {
        return Mockito.mock(Tenant.class);
    }
}
