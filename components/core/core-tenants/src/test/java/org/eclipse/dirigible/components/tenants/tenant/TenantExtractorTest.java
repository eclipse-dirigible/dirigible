/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * The subdomain strategy keeps resolving from the host, the token groups strategy resolves from the
 * tenant the user selected - and in that mode a request that names no usable tenant lands in the
 * default tenant instead of being refused, because the host carries no tenant to refuse.
 */
class TenantExtractorTest {

    private static final String TENANT_ID = "acme";
    private static final String TENANT_SUBDOMAIN = "acme";

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        TenantExtractor.TENANT_CACHE.invalidateAll();
        TenantExtractor.TENANT_ID_CACHE.invalidateAll();
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey());
        TenantExtractor.TENANT_CACHE.invalidateAll();
        TenantExtractor.TENANT_ID_CACHE.invalidateAll();
    }

    @Test
    void subdomainStrategyResolvesFromTheHost() {
        useStrategy("SUBDOMAIN");
        when(tenantService.findBySubdomain(TENANT_SUBDOMAIN)).thenReturn(Optional.of(provisionedTenant()));

        Optional<Tenant> tenant = newExtractor().determineTenant(requestFromHost(TENANT_SUBDOMAIN + ".dirigible.test"));

        assertEquals(TENANT_ID, tenant.orElseThrow()
                                      .getId());
    }

    @Test
    void subdomainStrategyStillRefusesAnUnregisteredHost() {
        useStrategy("SUBDOMAIN");
        when(tenantService.findBySubdomain(anyString())).thenReturn(Optional.empty());

        Optional<Tenant> tenant = newExtractor().determineTenant(requestFromHost("unregistered.dirigible.test"));

        assertTrue(tenant.isEmpty(), "an unregistered subdomain must keep producing the not-found response");
    }

    @Test
    void tokenGroupsStrategyResolvesTheSelectedTenant() {
        useStrategy("TOKEN_GROUPS");
        when(tenantService.findById(TENANT_ID)).thenReturn(Optional.of(provisionedTenant()));

        Optional<Tenant> tenant = newExtractor().determineTenant(requestWithSelectedTenant(TENANT_ID));

        assertEquals(TENANT_ID, tenant.orElseThrow()
                                      .getId());
    }

    @Test
    void tokenGroupsStrategyIgnoresTheHost() {
        useStrategy("TOKEN_GROUPS");
        when(tenantService.findById(TENANT_ID)).thenReturn(Optional.of(provisionedTenant()));
        HttpServletRequest request = requestWithSelectedTenant(TENANT_ID);
        when(request.getHeader("host")).thenReturn("some-other-tenant.dirigible.test");

        Optional<Tenant> tenant = newExtractor().determineTenant(request);

        assertEquals(TENANT_ID, tenant.orElseThrow()
                                      .getId());
        verify(tenantService, never()).findBySubdomain(anyString());
    }

    @Test
    void aRequestWithoutASessionLandsInTheDefaultTenant() {
        useStrategy("TOKEN_GROUPS");
        HttpServletRequest request = requestFromHost("unregistered.dirigible.test");
        when(request.getSession(false)).thenReturn(null);

        Optional<Tenant> tenant = newExtractor().determineTenant(request);

        assertTrue(tenant.orElseThrow()
                         .isDefault(),
                "a machine-to-machine or anonymous request carries no session and must not be refused");
    }

    @Test
    void aSessionWithoutASelectionLandsInTheDefaultTenant() {
        useStrategy("TOKEN_GROUPS");

        Optional<Tenant> tenant = newExtractor().determineTenant(requestWithSelectedTenant(null));

        assertTrue(tenant.orElseThrow()
                         .isDefault());
    }

    @Test
    void anUnknownSelectionLandsInTheDefaultTenant() {
        useStrategy("TOKEN_GROUPS");
        when(tenantService.findById("gone")).thenReturn(Optional.empty());

        Optional<Tenant> tenant = newExtractor().determineTenant(requestWithSelectedTenant("gone"));

        assertTrue(tenant.orElseThrow()
                         .isDefault(),
                "a selection that no longer resolves must not lock the user out of the instance");
    }

    @Test
    void aTenantThatIsNotProvisionedYetCannotBeEntered() {
        useStrategy("TOKEN_GROUPS");
        org.eclipse.dirigible.components.tenants.domain.Tenant tenantEntity = provisionedTenant();
        tenantEntity.setStatus(TenantStatus.INITIAL);
        when(tenantService.findById(TENANT_ID)).thenReturn(Optional.of(tenantEntity));

        Optional<Tenant> tenant = newExtractor().determineTenant(requestWithSelectedTenant(TENANT_ID));

        assertTrue(tenant.orElseThrow()
                         .isDefault());
    }

    @Test
    void theSelectedTenantIsCachedByIdAndEvictable() {
        useStrategy("TOKEN_GROUPS");
        when(tenantService.findById(TENANT_ID)).thenReturn(Optional.of(provisionedTenant()));
        TenantExtractor extractor = newExtractor();

        extractor.determineTenant(requestWithSelectedTenant(TENANT_ID));
        extractor.determineTenant(requestWithSelectedTenant(TENANT_ID));
        verify(tenantService, times(1)).findById(TENANT_ID);

        TenantExtractor.evictFromCaches(TENANT_ID, TENANT_SUBDOMAIN);
        extractor.determineTenant(requestWithSelectedTenant(TENANT_ID));
        verify(tenantService, times(2)).findById(TENANT_ID);
    }

    private TenantExtractor newExtractor() {
        return new TenantExtractor(tenantService);
    }

    private static void useStrategy(String strategy) {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue(strategy);
    }

    private static org.eclipse.dirigible.components.tenants.domain.Tenant provisionedTenant() {
        org.eclipse.dirigible.components.tenants.domain.Tenant tenant = new org.eclipse.dirigible.components.tenants.domain.Tenant("-",
                "Acme", "The Acme tenant", TENANT_SUBDOMAIN, TenantStatus.PROVISIONED);
        tenant.setId(TENANT_ID);
        return tenant;
    }

    private static HttpServletRequest requestFromHost(String host) {
        HttpServletRequest request = mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.getHeader("host")).thenReturn(host);
        return request;
    }

    private static HttpServletRequest requestWithSelectedTenant(String tenantId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).thenReturn(tenantId);
        return request;
    }
}
