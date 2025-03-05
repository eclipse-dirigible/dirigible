/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class DirigibleIntegrationTest extends IntegrationTest {

    @Autowired
    private TenantCreator tenantCreator;

    protected void createTenants(DirigibleTestTenant... tenants) {
        createTenants(Arrays.asList(tenants));
    }

    protected void createTenants(List<DirigibleTestTenant> tenants) {
        tenants.forEach(tenantCreator::createTenant);
    }

    protected void waitForTenantsProvisioning(List<DirigibleTestTenant> tenants) {
        tenants.forEach(this::waitForTenantProvisioning);
    }

    protected void waitForTenantProvisioning(DirigibleTestTenant tenant) {
        Awaitility.await()
                  .pollInterval(3, TimeUnit.SECONDS)
                  .atMost(35, TimeUnit.SECONDS)
                  .until(() -> tenantCreator.isTenantProvisioned(tenant));
    }
}
