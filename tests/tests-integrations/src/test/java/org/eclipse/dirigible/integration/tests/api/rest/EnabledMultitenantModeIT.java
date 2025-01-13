/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.rest;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.tests.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Multitenant mode must be enabled by default
 */
class EnabledMultitenantModeIT extends TenantDeterminationIT {

    private static String initialConfigValue;

    @BeforeAll
    public static void setUp() {
        initialConfigValue = Configuration.get(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey());
        Configuration.set(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey(), "true");
    }

    @AfterAll
    public static void tearDown() {
        Configuration.set(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey(), initialConfigValue);
    }

    @Test
    void testDefaultTenantResolution() {
        testHealthIsAccessible("localhost", null);
        testHealthIsAccessible("default.localhost", null);
    }

    @Test
    void testHostWitchDoesNotMatchTheDefaultRegexIsResolvedAsDefaultTenant() {
        testHealthIsAccessible("host-which-does-not-match-the-default-tenant-regex", null);
    }

    @Test
    void testRegisteredTenantResolution() {
        DirigibleTestTenant tenant1 = new DirigibleTestTenant("test-tenant-1");
        createTenants(tenant1);
        waitForTenantProvisioning(tenant1);

        testHealthIsAccessible(tenant1.getHost(), null);
        testHealthIsAccessible("212.39.89.114", tenant1.getHost());
    }

    @Test
    void testUnregisteredTenantResolution() {
        testHealthIsNotAccessible("unregistered-tenant.localhost", null);
        testHealthIsNotAccessible("212.39.89.114", "unregistered-tenant.localhost");
    }

}
