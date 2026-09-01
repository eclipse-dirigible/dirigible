/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningStep;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantFactory;
import org.junit.jupiter.api.Test;

/**
 * The built-in provisioner owns {@link TenantStatus#INITIAL} tenants and nothing else.
 *
 * <p>
 * This is the invariant that lets an external provisioner own a tenant end to end: a tenant it
 * registered as {@link TenantStatus#PENDING_ACTIVATION} must never be picked up here, or the
 * platform would create a database user and a schema of its own alongside the ones the external
 * provisioner created - two owners for one tenant's data, and a data source pointing at whichever
 * of them won.
 */
class TenantsProvisionerTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final TenantProvisioningStep provisioningStep = mock(TenantProvisioningStep.class);
    private final TenantPostProvisioningStep postProvisioningStep = mock(TenantPostProvisioningStep.class);

    private final TenantsProvisioner provisioner =
            new TenantsProvisioner(tenantService, Set.of(provisioningStep), Set.of(postProvisioningStep), new TenantFactory());

    @Test
    void onlyInitialTenantsAreQueried() {
        when(tenantService.findByStatus(TenantStatus.INITIAL)).thenReturn(Set.of(tenant("acme", TenantStatus.INITIAL)));

        provisioner.provision();

        verify(tenantService).findByStatus(TenantStatus.INITIAL);
        verify(tenantService, never()).findByStatus(TenantStatus.PENDING_ACTIVATION);
        verify(tenantService, never()).findByStatus(TenantStatus.PROVISIONED);
    }

    @Test
    void anInitialTenantIsProvisionedAndMarkedProvisioned() {
        Tenant acme = tenant("acme", TenantStatus.INITIAL);
        when(tenantService.findByStatus(TenantStatus.INITIAL)).thenReturn(Set.of(acme));

        provisioner.provision();

        verify(provisioningStep).execute(any());
        verify(postProvisioningStep).execute();
        verify(tenantService).save(acme);
        assertEquals(TenantStatus.PROVISIONED, acme.getStatus());
    }

    /**
     * The externally provisioned tenant is invisible: the provisioner finds nothing to do, so it runs
     * no provisioning step against it and - since nothing was provisioned - no post-provisioning step
     * either.
     */
    @Test
    void aPendingActivationTenantIsNeverProvisioned() {
        Tenant pending = tenant("globex", TenantStatus.PENDING_ACTIVATION);
        when(tenantService.findByStatus(TenantStatus.INITIAL)).thenReturn(Set.of());
        when(tenantService.findByStatus(TenantStatus.PENDING_ACTIVATION)).thenReturn(Set.of(pending));

        provisioner.provision();

        verify(provisioningStep, never()).execute(any());
        verify(postProvisioningStep, never()).execute();
        verify(tenantService, never()).save(any());
        assertEquals(TenantStatus.PENDING_ACTIVATION, pending.getStatus());
    }

    private static Tenant tenant(String id, TenantStatus status) {
        Tenant tenant = new Tenant("-", id, "", id, status);
        tenant.setId(id);
        return tenant;
    }
}
