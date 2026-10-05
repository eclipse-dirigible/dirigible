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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Set;

import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The delayed provisioning belongs to the context that scheduled it: once that context is closed, a
 * provisioning that has not started must never run.
 */
class TenantsInitializerTest {

    private static final Duration DELAY = Duration.ofMillis(200);

    private final TenantService tenantService = mock(TenantService.class);

    private final TenantsInitializer initializer =
            new TenantsInitializer(new TenantsProvisioner(tenantService, Set.of(), Set.of(), new TenantFactory()), DELAY);

    @AfterEach
    void shutDown() {
        initializer.destroy();
    }

    @Test
    void provisionsTheTenantsAfterTheDelay() {
        initializer.onApplicationEvent(null);

        verify(tenantService, timeout(5_000)).findByStatus(TenantStatus.INITIAL);
    }

    @Test
    void aProvisioningScheduledBeforeTheContextClosedNeverRuns() {
        initializer.onApplicationEvent(null);

        initializer.destroy();

        verify(tenantService, after(DELAY.multipliedBy(5)
                                         .toMillis()).never()).findByStatus(any());
    }

}
