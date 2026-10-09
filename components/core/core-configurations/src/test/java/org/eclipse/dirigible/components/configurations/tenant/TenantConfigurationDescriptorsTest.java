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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.ConfigGroup;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.domain.TenantConfiguration;
import org.eclipse.dirigible.components.configurations.domain.TenantConfigurationDescriptor;
import org.eclipse.dirigible.components.configurations.domain.TenantConfigurationGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The grouped tenant configuration: the allow-listed keys, grouped, with the tenant's value next to
 * the platform value it overrides.
 */
class TenantConfigurationDescriptorsTest {

    private TenantConfigurationService service;

    @BeforeEach
    void setUp() throws Exception {
        TenantConfigurationStore store = mock(TenantConfigurationStore.class);
        when(store.readAll()).thenReturn(Map.of("DIRIGIBLE_BRANDING_NAME", "Acme", "DIRIGIBLE_DATABASE_SYSTEM_URL", "inert"));
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn("acme");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        TenantConfigurationKeyPolicy policy = new TenantConfigurationKeyPolicy();
        policy.registerWithDescriptors();
        service = new TenantConfigurationService(store, new TenantConfigurationCache(), policy, tenantContext);
        Configuration.set("DIRIGIBLE_BRANDING_NAME", "Platform");
    }

    @AfterEach
    void cleanUp() {
        Configuration.remove("DIRIGIBLE_BRANDING_NAME");
        ConfigDescriptors.setTenantOverridablePolicy(null);
    }

    @Test
    void predefinedFiltersByGroup() throws Exception {
        List<TenantConfiguration> branding = service.listPredefinedForCurrentTenant(Set.of(ConfigGroup.BRANDING));
        assertEquals(8, branding.size());
        assertTrue(branding.stream()
                           .allMatch(entry -> entry.key()
                                                   .startsWith("DIRIGIBLE_BRANDING_")));
        assertEquals(service.listPredefinedForCurrentTenant(), service.listPredefinedForCurrentTenant(null));
    }

    @Test
    void descriptorsGroupTheOverridableKeysWithThePlatformValue() throws Exception {
        List<TenantConfigurationGroup> groups = service.describeForCurrentTenant(null);
        assertEquals(List.of("instance", "branding", "locale", "documents"), groups.stream()
                                                                                   .map(TenantConfigurationGroup::group)
                                                                                   .toList());
        TenantConfigurationDescriptor name = groups.get(1)
                                                   .entries()
                                                   .get(0);
        assertEquals("DIRIGIBLE_BRANDING_NAME", name.key());
        assertEquals("Acme", name.value());
        assertEquals("Platform", name.defaultValue());
        TenantConfigurationDescriptor languages = groups.get(2)
                                                        .entries()
                                                        .get(0);
        assertEquals("DIRIGIBLE_APPLICATION_LANGUAGES", languages.key());
        assertNull(languages.value());
        assertEquals("en", languages.defaultValue());
    }

    @Test
    void groupsCountTheTenantsOwnValues() throws Exception {
        ConfigDescriptors.GroupSummary branding = service.listGroupsForCurrentTenant()
                                                         .stream()
                                                         .filter(group -> group.id()
                                                                               .equals("branding"))
                                                         .findFirst()
                                                         .orElseThrow();
        assertEquals(8, branding.total());
        assertEquals(1, branding.set());
    }

    @Test
    void thePlatformViewFlagsTheOverridableKeys() {
        assertTrue(ConfigDescriptors.describe("DIRIGIBLE_BRANDING_NAME")
                                    .tenantOverridable());
    }

}
