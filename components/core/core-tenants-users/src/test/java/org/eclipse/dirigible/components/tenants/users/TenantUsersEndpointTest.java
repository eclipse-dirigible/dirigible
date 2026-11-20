/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantUsersEndpointTest {

    private static final String TENANT_ID = "6f1c2a9e-4b7d-4e0a-9c3f-2d8b5a1e7f40";

    private TenantUsersAccess access;
    private TenantService tenants;
    private TenantUsersEndpoint endpoint;

    @BeforeEach
    void setUp() {
        access = mock(TenantUsersAccess.class);
        tenants = mock(TenantService.class);
        TenantUserRoles roles = mock(TenantUserRoles.class);
        when(roles.catalog()).thenReturn(List.of(new TenantUserRoles.Offer("Owner", null, null)));
        endpoint = new TenantUsersEndpoint(access, mock(TenantUserReplicaService.class), mock(TenantUserCommands.class), roles, tenants);
    }

    private void inTenant(String tenantId) {
        when(access.canRead()).thenReturn(true);
        when(access.canManage()).thenReturn(true);
        when(access.requireTenant()).thenReturn(tenantId);
    }

    private static Tenant registered(String name) {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName(name);
        return tenant;
    }

    @Test
    void contextCarriesTheTenantName() {
        inTenant(TENANT_ID);
        when(tenants.findById(TENANT_ID)).thenReturn(Optional.of(registered("Acme Production")));

        TenantUsersContext context = endpoint.context()
                                             .getBody();

        assertEquals(TENANT_ID, context.tenantId());
        assertEquals("Acme Production", context.tenantName());
    }

    @Test
    void contextFallsBackToTheIdWhenTheTenantHasNoName() {
        inTenant(TENANT_ID);
        when(tenants.findById(TENANT_ID)).thenReturn(Optional.of(registered(" ")));

        assertEquals(TENANT_ID, endpoint.context()
                                        .getBody()
                                        .tenantName());

        when(tenants.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertEquals(TENANT_ID, endpoint.context()
                                        .getBody()
                                        .tenantName());
    }

    @Test
    void contextHasNoTenantNameInTheDefaultTenant() {
        when(access.canRead()).thenReturn(false);

        TenantUsersContext context = endpoint.context()
                                             .getBody();

        assertNull(context.tenantId());
        assertNull(context.tenantName());
        verifyNoInteractions(tenants);
    }
}
