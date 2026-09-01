/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Which tenants may be deleted. A tenant that never materialized anything can be taken back; a
 * provisioned one cannot, because deleting it would be deprovisioning without deprovisioning steps.
 */
class TenantEndpointTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);
    private final TenantEndpoint endpoint = new TenantEndpoint(tenantService, userService);

    @Test
    void anInitialTenantIsDeleted() throws URISyntaxException {
        assertDeleted(TenantStatus.INITIAL);
    }

    /** The rollback path of the tenant provisioning API. */
    @Test
    void aPendingActivationTenantIsDeleted() throws URISyntaxException {
        assertDeleted(TenantStatus.PENDING_ACTIVATION);
    }

    @Test
    void aProvisionedTenantIsRefused() throws URISyntaxException {
        Tenant tenant = tenant(TenantStatus.PROVISIONED);
        when(tenantService.findById("acme")).thenReturn(Optional.of(tenant));

        ResponseEntity<String> response = endpoint.deleteTenant("acme");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(tenantService, never()).delete(any());
        verify(userService, never()).deleteUser(any());
    }

    private void assertDeleted(TenantStatus status) throws URISyntaxException {
        Tenant tenant = tenant(status);
        when(tenantService.findById("acme")).thenReturn(Optional.of(tenant));
        when(userService.findUsersByTenantId("acme")).thenReturn(List.of());

        ResponseEntity<String> response = endpoint.deleteTenant("acme");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(tenantService).delete(tenant);
    }

    private static Tenant tenant(TenantStatus status) {
        Tenant tenant = new Tenant("-", "Acme", "", "acme", status);
        tenant.setId("acme");
        return tenant;
    }
}
