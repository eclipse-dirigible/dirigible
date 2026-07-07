/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the tenant-aware configuration feature. Exercises the full chain in the
 * running application (per-tenant DIRIGIBLE_CONFIGURATION table -> store -> cache -> key policy ->
 * thread-scoped {@link Configuration} precedence) against the default tenant.
 */
class TenantConfigurationIT extends IntegrationTest {

    /** Matches the default allow-list ({@code DIRIGIBLE_TENANT_*}). */
    private static final String ALLOWED_KEY = "DIRIGIBLE_TENANT_IT_FEATURE";

    /** A real configuration key that is NOT in the default allow-list, so it must not be injected. */
    private static final String NON_ALLOWLISTED_KEY = "DIRIGIBLE_MAIL_SMTPS_HOST";

    @Autowired
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    @AfterEach
    void cleanup() throws Exception {
        tenantContext.execute(defaultTenant, () -> {
            tenantConfigurationService.delete(ALLOWED_KEY);
            tenantConfigurationService.delete(NON_ALLOWLISTED_KEY);
            return null;
        });
    }

    @Test
    void allowlistedTenantConfigResolvesWithPrecedenceAndClears() throws Exception {
        tenantContext.execute(defaultTenant, () -> {
            tenantConfigurationService.set(ALLOWED_KEY, "tenant-value");

            Map<String, String> injectable = tenantConfigurationService.resolveInjectableForCurrentTenant();
            assertEquals("tenant-value", injectable.get(ALLOWED_KEY));

            Configuration.setThreadConfiguration(injectable);
            try {
                assertEquals("tenant-value", Configuration.get(ALLOWED_KEY));
            } finally {
                Configuration.removeThreadConfiguration();
            }
            // once the thread configuration is cleared, the value is no longer resolvable
            assertNull(Configuration.get(ALLOWED_KEY));
            return null;
        });
    }

    @Test
    void nonAllowlistedKeyIsStoredButNeverInjected() throws Exception {
        tenantContext.execute(defaultTenant, () -> {
            tenantConfigurationService.set(NON_ALLOWLISTED_KEY, "smtp.tenant.example");

            // it is persisted for the tenant ...
            assertTrue(tenantConfigurationService.listForCurrentTenant()
                                                 .stream()
                                                 .anyMatch(entry -> NON_ALLOWLISTED_KEY.equals(entry.key())));
            // ... but the key policy keeps it out of the injectable set
            assertFalse(tenantConfigurationService.resolveInjectableForCurrentTenant()
                                                  .containsKey(NON_ALLOWLISTED_KEY));
            return null;
        });
    }

}
