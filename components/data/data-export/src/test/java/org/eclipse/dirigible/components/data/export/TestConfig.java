/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @MockBean
    @DefaultTenant
    private Tenant defaultTenant;

    @Bean
    TenantContext createTenantContext() {
        TenantContext context = Mockito.mock(TenantContext.class);
        when(context.isInitialized()).thenReturn(false);
        when(context.isNotInitialized()).thenReturn(true);
        return context;
    }
}
