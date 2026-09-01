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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.Test;

/**
 * The per-tenant fan-out reaches provisioned tenants and the default tenant, and nothing else.
 *
 * <p>
 * Every multitenant synchronizer materializes its artefacts through this fan-out, so a tenant it
 * includes is a tenant whose schema is about to be written to. A
 * {@link TenantStatus#PENDING_ACTIVATION} tenant has no data source registered yet - it is being
 * provisioned from the outside - so including it would fail every artefact of the pass. This is the
 * second half of the invariant {@code TenantsProvisionerTest} pins from the other side.
 */
class TenantContextImplTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final TenantContextImpl tenantContext = new TenantContextImpl(tenantService);

    @Test
    void theFanOutCoversProvisionedTenantsAndTheDefaultOne() {
        when(tenantService.findByStatus(TenantStatus.PROVISIONED)).thenReturn(Set.of(tenant("acme", TenantStatus.PROVISIONED)));

        Set<String> visited = new HashSet<>();
        tenantContext.executeForEachTenant(() -> visited.add(tenantContext.getCurrentTenant()
                                                                          .getId()));

        assertEquals(2, visited.size(), "expected the provisioned tenant and the default one, got " + visited);
        assertTrue(visited.contains("acme"));
        assertTrue(visited.contains(TenantImpl.getDefaultTenant()
                                              .getId()));
    }

    @Test
    void aPendingActivationTenantIsNeverVisited() {
        when(tenantService.findByStatus(TenantStatus.PROVISIONED)).thenReturn(Set.of());
        when(tenantService.findByStatus(TenantStatus.PENDING_ACTIVATION)).thenReturn(
                Set.of(tenant("globex", TenantStatus.PENDING_ACTIVATION)));

        Set<String> visited = new HashSet<>();
        tenantContext.executeForEachTenant(() -> visited.add(tenantContext.getCurrentTenant()
                                                                          .getId()));

        assertFalse(visited.contains("globex"));
        verify(tenantService).findByStatus(TenantStatus.PROVISIONED);
        verify(tenantService, never()).findByStatus(TenantStatus.INITIAL);
    }

    private static Tenant tenant(String id, TenantStatus status) {
        Tenant tenant = new Tenant("-", id, "", id, status);
        tenant.setId(id);
        return tenant;
    }
}
