/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class MultitenancyUserInterfaceIntegrationTest extends UserInterfaceIntegrationTest {

    @BeforeAll
    public static void setUp() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
    }

    protected List<DirigibleTestTenant> provisionTenants() {
        DirigibleTestTenant defaultTenant = DirigibleTestTenant.createDefaultTenant();
        DirigibleTestTenant tenant1 = new DirigibleTestTenant("test-tenant-1");
        DirigibleTestTenant tenant2 = new DirigibleTestTenant("test-tenant-2");

        List<DirigibleTestTenant> tenants = List.of(defaultTenant, tenant1, tenant2);

        createTenants(tenants);

        waitForTenantsProvisioning(tenants);

        return tenants;
    }

}
