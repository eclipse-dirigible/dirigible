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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * A user enters a tenant their own groups grant them, and gets the roles of that tenant on top of
 * their global ones. The identity provider stays the authority on membership: a tenant the groups
 * do not name is refused, and one the groups lost is dropped again on the next request.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSelectionManagerTest {

    private static final String GROUPS_CLAIM = "groups";
    private static final String APP_ID = "library";
    private static final String USER = "owner@example.com";
    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";

    @Mock
    private TenantService tenantService;

    @Mock
    private SecurityContextRepository securityContextRepository;

    private TenantSelectionManager manager;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_GROUPS_CLAIM.setStringValue(GROUPS_CLAIM);
        DirigibleConfig.APP_ID.setStringValue(APP_ID);
        manager = new TenantSelectionManager(new TenantGroupsClaim(), tenantService, securityContextRepository);
        request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Configuration.remove(DirigibleConfig.TENANT_GROUPS_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.APP_ID.getKey());
    }

    @Test
    void theTenantsOfTheGroupsAreOfferedWithTheirLocalState() {
        authenticate("acme.library.Owner", "globex.library.User", "acme.bi.Owner", "DEVELOPER");
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.PROVISIONED)));
        when(tenantService.findById(GLOBEX)).thenReturn(Optional.empty());

        List<TenantOption> tenants = manager.availableTenants(SecurityContextHolder.getContext()
                                                                                   .getAuthentication());

        assertThat(tenants).containsExactly(new TenantOption(ACME, "Acme Ltd", true), new TenantOption(GLOBEX, GLOBEX, false));
    }

    @Test
    void selectingATenantStoresItAndGrantsItsRolesOnTopOfTheGlobalOnes() {
        authenticate("acme.library.Owner", "acme.library.User", "globex.library.User", "DEVELOPER");
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.PROVISIONED)));

        Set<String> roles = manager.selectTenant(request, response, ACME);

        assertThat(roles).containsExactlyInAnyOrder("DEVELOPER", "Owner", "User");
        assertThat(request.getSession()
                          .getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).isEqualTo(ACME);
        assertThat(currentRoleNames()).containsExactlyInAnyOrder("DEVELOPER", "Owner", "User");
        verify(securityContextRepository).saveContext(any(SecurityContext.class), any(), any());
    }

    @Test
    void switchingTenantReplacesTheSelectionAndTheRoles() {
        authenticate("acme.library.Owner", "globex.library.User");
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.PROVISIONED)));
        when(tenantService.findById(GLOBEX)).thenReturn(Optional.of(tenant(GLOBEX, "Globex", TenantStatus.PROVISIONED)));

        manager.selectTenant(request, response, ACME);
        Set<String> roles = manager.selectTenant(request, response, GLOBEX);

        assertThat(roles).containsExactly("User");
        assertThat(request.getSession()
                          .getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).isEqualTo(GLOBEX);
        assertThat(currentRoleNames()).containsExactly("User");
    }

    @Test
    void aTenantTheGroupsDoNotGrantIsRefused() {
        authenticate("acme.library.Owner");

        assertThatThrownBy(
                () -> manager.selectTenant(request, response, "someone-elses-tenant")).isInstanceOf(TenantSelectionException.class)
                                                                                      .extracting("reason")
                                                                                      .isEqualTo(
                                                                                              TenantSelectionException.Reason.NOT_A_MEMBER);
        assertThat(request.getSession()
                          .getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void aTenantOfAnotherApplicationIsNotAMemberEither() {
        authenticate("acme.bi.Owner");

        assertThatThrownBy(() -> manager.selectTenant(request, response, ACME)).isInstanceOf(TenantSelectionException.class);
    }

    @Test
    void aTenantThisInstanceHasNotProvisionedYetIsRefused() {
        authenticate("acme.library.Owner");
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.INITIAL)));

        assertThatThrownBy(() -> manager.selectTenant(request, response, ACME)).isInstanceOf(TenantSelectionException.class)
                                                                               .extracting("reason")
                                                                               .isEqualTo(
                                                                                       TenantSelectionException.Reason.NOT_PROVISIONED_HERE);
    }

    @Test
    void onlyALoggedInUserCanSelect() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> manager.selectTenant(request, response, ACME)).isInstanceOf(TenantSelectionException.class)
                                                                               .extracting("reason")
                                                                               .isEqualTo(
                                                                                       TenantSelectionException.Reason.NOT_AN_INTERACTIVE_SESSION);
    }

    @Test
    void authoritiesLostToATokenRefreshAreReApplied() {
        authenticate("acme.library.Owner", "DEVELOPER");
        when(tenantService.findById(ACME)).thenReturn(Optional.of(tenant(ACME, "Acme Ltd", TenantStatus.PROVISIONED)));
        manager.selectTenant(request, response, ACME);
        // What a refresh leaves behind: the mapper granted the global roles only.
        authenticate(Set.of("DEVELOPER"), "acme.library.Owner", "DEVELOPER");

        manager.ensureConsistent(request, response);

        assertThat(currentRoleNames()).containsExactlyInAnyOrder("DEVELOPER", "Owner");
    }

    @Test
    void consistentAuthoritiesAreLeftAlone() {
        authenticate(Set.of("DEVELOPER", "Owner"), "acme.library.Owner", "DEVELOPER");
        request.getSession()
               .setAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE, ACME);

        manager.ensureConsistent(request, response);

        verify(securityContextRepository, never()).saveContext(any(SecurityContext.class), any(), any());
    }

    @Test
    void aSelectionTheGroupsNoLongerGrantIsDropped() {
        // The Owner group of acme was revoked at the identity provider.
        authenticate(Set.of("DEVELOPER", "Owner"), "DEVELOPER");
        request.getSession()
               .setAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE, ACME);

        manager.ensureConsistent(request, response);

        assertThat(request.getSession()
                          .getAttribute(TenantSelectionConstants.SELECTED_TENANT_ID_SESSION_ATTRIBUTE)).isNull();
        assertThat(currentRoleNames()).containsExactly("DEVELOPER");
    }

    @Test
    void aRequestWithoutASelectionNeedsNoRepair() {
        authenticate("acme.library.Owner");

        manager.ensureConsistent(request, response);

        verify(securityContextRepository, never()).saveContext(any(SecurityContext.class), any(), any());
    }

    private void authenticate(String... groups) {
        authenticate(Set.of(), groups);
    }

    private void authenticate(Set<String> currentRoles, String... groups) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", USER, GROUPS_CLAIM, List.of(groups)));
        OidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        Authentication authentication = new OAuth2AuthenticationToken(oidcUser, currentRoles.stream()
                                                                                            .map(role -> new SimpleGrantedAuthority(
                                                                                                    "ROLE_" + role))
                                                                                            .toList(),
                "keycloak");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Set<String> currentRoleNames() {
        return Set.copyOf(AuthoritiesUtil.toRoleNames(SecurityContextHolder.getContext()
                                                                           .getAuthentication()
                                                                           .getAuthorities()));
    }

    private static Tenant tenant(String id, String name, TenantStatus status) {
        Tenant tenant = new Tenant("-", name, "The " + name + " tenant", id, status);
        tenant.setId(id);
        return tenant;
    }
}
