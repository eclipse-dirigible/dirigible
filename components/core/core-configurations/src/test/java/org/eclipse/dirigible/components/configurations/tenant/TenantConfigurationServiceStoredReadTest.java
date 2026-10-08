/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.junit.jupiter.api.Test;

/**
 * {@link TenantConfigurationService#readStoredForCurrentTenant(String)} reads past this node's
 * cache, which another node's write does not invalidate, and refreshes it.
 */
class TenantConfigurationServiceStoredReadTest {

    @Test
    void aStoredReadSeesAnotherNodesWriteAndRefreshesTheCache() throws Exception {
        TenantConfigurationStore store = mock(TenantConfigurationStore.class);
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("t1");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        TenantConfigurationService service = new TenantConfigurationService(store, new TenantConfigurationCache(),
                mock(TenantConfigurationKeyPolicy.class), tenantContext);
        when(store.readAll()).thenReturn(Map.of("KEY", "old"));
        assertEquals("old", service.listForCurrentTenant()
                                   .get(0)
                                   .value());

        // Another node writes: this node's cache still holds the old value.
        when(store.readAll()).thenReturn(Map.of("KEY", "new"));
        assertEquals("old", service.listForCurrentTenant()
                                   .get(0)
                                   .value());

        assertEquals("new", service.readStoredForCurrentTenant("KEY"));
        assertNull(service.readStoredForCurrentTenant("OTHER"));
        assertEquals("new", service.listForCurrentTenant()
                                   .get(0)
                                   .value(),
                "the stored read refreshes this node's cache");
    }
}
