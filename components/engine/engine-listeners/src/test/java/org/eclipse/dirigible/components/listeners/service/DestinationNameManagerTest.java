/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.Test;

/**
 * The physical-name rule that every messaging call site depends on. It had no direct test, which is
 * how the client-Java subscriber came to bypass it altogether while the producer applied it.
 */
class DestinationNameManagerTest {

    private static final String LOGICAL_NAME = "orders";

    @Test
    void outsideAnyTenantContextTheNameIsUnchanged() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);

        assertEquals(LOGICAL_NAME, new DestinationNameManager(tenantContext).toTenantName(LOGICAL_NAME));
    }

    @Test
    void theDefaultTenantIsNotPrefixed() {
        assertEquals(LOGICAL_NAME, toTenantName("default-tenant", true),
                "a single-tenant deployment must see the logical name on the broker");
    }

    @Test
    void anyOtherTenantIsPrefixedWithItsId() {
        assertEquals("acme###" + LOGICAL_NAME, toTenantName("acme", false));
    }

    private static String toTenantName(String tenantId, boolean defaultTenant) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.isDefault()).thenReturn(defaultTenant);
        if (!defaultTenant) {
            when(tenant.getId()).thenReturn(tenantId);
        }
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        return new DestinationNameManager(tenantContext).toTenantName(LOGICAL_NAME);
    }
}
