/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.dirigible.components.configurations.domain.TenantConfiguration;
import org.eclipse.dirigible.components.configurations.service.SensitiveConfigurations;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The tenant configuration endpoints return sensitive values masked and never store the mask echoed
 * back by an unchanged edit.
 */
public class TenantConfigurationsEndpointTest {

    private static final List<TenantConfiguration> STORED = List.of(new TenantConfiguration("DIRIGIBLE_TENANT_SMTP_PASSWORD", "p4ss"),
            new TenantConfiguration("DIRIGIBLE_TENANT_API_TOKEN", "tok-123"),
            new TenantConfiguration("DIRIGIBLE_TENANT_UNSET_SECRET", null), new TenantConfiguration("DIRIGIBLE_BRANDING_NAME", "Acme"));

    private TenantConfigurationService service;

    private TenantConfigurationsEndpoint endpoint;

    @BeforeEach
    public void setUp() throws Exception {
        service = mock(TenantConfigurationService.class);
        when(service.listForCurrentTenant()).thenReturn(STORED);
        when(service.listPredefinedForCurrentTenant()).thenReturn(STORED);
        when(service.listPredefinedForCurrentTenant(any())).thenReturn(STORED);
        endpoint = new TenantConfigurationsEndpoint(service);
    }

    @Test
    public void findAllMasksSensitiveValues() {
        assertMasked(endpoint.findAll()
                             .getBody());
    }

    @Test
    public void findPredefinedMasksSensitiveValues() {
        assertMasked(endpoint.findPredefined(null)
                             .getBody());
    }

    @Test
    public void findPredefinedOfAGroupMasksSensitiveValues() {
        assertMasked(endpoint.findPredefined(List.of("branding"))
                             .getBody());
    }

    @Test
    public void setIgnoresTheEchoedMaskOfASensitiveKey() throws Exception {
        endpoint.set(new TenantConfiguration("DIRIGIBLE_TENANT_SMTP_PASSWORD", SensitiveConfigurations.MASK));
        verify(service, never()).set(any(), any());
    }

    @Test
    public void setStoresANewSensitiveValue() throws Exception {
        endpoint.set(new TenantConfiguration("DIRIGIBLE_TENANT_SMTP_PASSWORD", "n3w"));
        verify(service).set("DIRIGIBLE_TENANT_SMTP_PASSWORD", "n3w");
    }

    private static void assertMasked(List<TenantConfiguration> body) {
        assertEquals(List.of(new TenantConfiguration("DIRIGIBLE_TENANT_SMTP_PASSWORD", SensitiveConfigurations.MASK),
                new TenantConfiguration("DIRIGIBLE_TENANT_API_TOKEN", SensitiveConfigurations.MASK),
                new TenantConfiguration("DIRIGIBLE_TENANT_UNSET_SECRET", null), new TenantConfiguration("DIRIGIBLE_BRANDING_NAME", "Acme")),
                body);
        assertFalse(body.toString()
                        .contains("p4ss"));
        assertFalse(body.toString()
                        .contains("tok-123"));
    }

}
