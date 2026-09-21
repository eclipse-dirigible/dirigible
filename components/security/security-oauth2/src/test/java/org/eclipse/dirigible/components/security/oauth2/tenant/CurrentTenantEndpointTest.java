/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.security.oauth2.tenant.CurrentTenantEndpoint.CurrentTenantState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/**
 * The endpoint is the header widget's contract: it names the tenant of the request on every
 * resolution strategy, says whether the instance is multitenant at all - which is what decides
 * whether the widget is shown - and offers a switch only where one can be made.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CurrentTenantEndpointTest {

    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";
    private static final String DEFAULT_TENANT = "default-tenant";

    @Mock
    private TenantSelectionManager tenantSelectionManager;

    @Mock
    private TenantContext tenantContext;

    private CurrentTenantEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = new CurrentTenantEndpoint(tenantSelectionManager, tenantContext);
        currentTenantIs(DEFAULT_TENANT, DEFAULT_TENANT, true);
    }

    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey());
        SecurityContextHolder.clearContext();
    }

    @Test
    void aSingleTenantInstanceReportsTheDefaultTenantAndNothingToShow() {
        subdomainResolution(false);
        authenticateInteractively();

        CurrentTenantState state = current();

        assertThat(state.tenant()
                        .id()).isEqualTo(DEFAULT_TENANT);
        assertThat(state.tenant()
                        .defaultTenant()).isTrue();
        assertThat(state.multitenant()).isFalse();
        assertThat(state.resolutionStrategy()).isEqualTo("SUBDOMAIN");
        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).isEmpty();
    }

    /**
     * Under {@code SUBDOMAIN} the tenant is the host's: it is named, and there is nothing to switch to
     * from here. The groups are never consulted - they say nothing about tenants in this mode, and the
     * manager warns about tenants it does not know, which a page load must not trigger.
     */
    @Test
    void aSubdomainInstanceNamesTheTenantOfTheHostAndOffersNoSwitch() {
        subdomainResolution(true);
        currentTenantIs(ACME, "Acme Ltd", false);
        authenticateInteractively();

        CurrentTenantState state = current();

        assertThat(state.tenant()
                        .id()).isEqualTo(ACME);
        assertThat(state.tenant()
                        .name()).isEqualTo("Acme Ltd");
        assertThat(state.tenant()
                        .defaultTenant()).isFalse();
        assertThat(state.multitenant()).isTrue();
        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).isEmpty();
        verify(tenantSelectionManager, never()).availableTenants(any());
    }

    @Test
    void aUserOfSeveralReadyTenantsMaySwitch() {
        tokenGroupsResolution();
        currentTenantIs(ACME, "Acme Ltd", false);
        authenticateInteractively();
        offered(ready(ACME, "Acme Ltd"), ready(GLOBEX, "Globex Corporation"));

        CurrentTenantState state = current();

        assertThat(state.multitenant()).isTrue();
        assertThat(state.resolutionStrategy()).isEqualTo("TOKEN_GROUPS");
        assertThat(state.switchable()).isTrue();
        assertThat(state.tenants()).extracting(TenantOption::id)
                                   .containsExactly(ACME, GLOBEX);
    }

    /**
     * A tenant that cannot be entered is still listed - the picker shows it too, with the reason - but
     * it is no switch target, so a user whose only other tenant is one of those has nowhere to go.
     */
    @Test
    void aTenantThatCannotBeEnteredIsListedButIsNoSwitchTarget() {
        tokenGroupsResolution();
        currentTenantIs(ACME, "Acme Ltd", false);
        authenticateInteractively();
        offered(ready(ACME, "Acme Ltd"), new TenantOption(GLOBEX, GLOBEX, false, TenantOption.State.UNKNOWN),
                new TenantOption("initech", "Initech", false, TenantOption.State.PREPARING));

        CurrentTenantState state = current();

        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).hasSize(3);
    }

    @Test
    void aUserOfOneTenantHasNothingToSwitchTo() {
        tokenGroupsResolution();
        currentTenantIs(ACME, "Acme Ltd", false);
        authenticateInteractively();
        offered(ready(ACME, "Acme Ltd"));

        CurrentTenantState state = current();

        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).hasSize(1);
    }

    /**
     * Staff of the instance - global roles, no tenant of their own - work in the default tenant, and
     * the widget says so without offering a menu.
     */
    @Test
    void staffWithoutATenantAreInTheDefaultTenant() {
        tokenGroupsResolution();
        authenticateInteractively();
        offered();

        CurrentTenantState state = current();

        assertThat(state.tenant()
                        .id()).isEqualTo(DEFAULT_TENANT);
        assertThat(state.tenant()
                        .defaultTenant()).isTrue();
        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).isEmpty();
    }

    /**
     * A request that is not an interactive session - a bearer token, basic authentication - names its
     * tenant per request or has none; there is no session to switch in, so nothing is offered.
     */
    @Test
    void aRequestWithoutAnInteractiveSessionIsNeverOfferedASwitch() {
        tokenGroupsResolution();
        currentTenantIs(ACME, "Acme Ltd", false);
        SecurityContextHolder.getContext()
                             .setAuthentication(new UsernamePasswordAuthenticationToken("service", "n/a",
                                     List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

        CurrentTenantState state = current();

        assertThat(state.tenant()
                        .id()).isEqualTo(ACME);
        assertThat(state.switchable()).isFalse();
        assertThat(state.tenants()).isEmpty();
        verify(tenantSelectionManager, never()).availableTenants(any());
    }

    private CurrentTenantState current() {
        return endpoint.current()
                       .getBody();
    }

    private void currentTenantIs(String id, String name, boolean defaultTenant) {
        Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
        when(tenant.getId()).thenReturn(id);
        when(tenant.getName()).thenReturn(name);
        when(tenant.isDefault()).thenReturn(defaultTenant);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
    }

    private void offered(TenantOption... options) {
        when(tenantSelectionManager.availableTenants(any())).thenReturn(List.of(options));
    }

    private static TenantOption ready(String id, String name) {
        return new TenantOption(id, name, true, TenantOption.State.READY);
    }

    private static void subdomainResolution(boolean multitenant) {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("SUBDOMAIN");
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(multitenant);
    }

    private static void tokenGroupsResolution() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
    }

    /** What an OIDC login leaves behind: the authentication the selection endpoint acts on. */
    private static void authenticateInteractively() {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", "owner@example.com"));
        Authentication authentication =
                new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken),
                        List.of(new SimpleGrantedAuthority("ROLE_USER")), "keycloak");
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
    }
}
