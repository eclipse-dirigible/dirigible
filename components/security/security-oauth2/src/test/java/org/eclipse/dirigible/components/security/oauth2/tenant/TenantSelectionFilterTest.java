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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.groups.UserTenantAssignments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The decision the filter makes on every interactive request: one tenant is entered silently,
 * several send the user to the picker (a browser by redirect, a programmatic caller by a conflict),
 * none is fine for staff and refused for everyone else, and a selection that already exists is kept
 * consistent.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSelectionFilterTest {

    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";

    @Mock
    private TenantSelectionManager tenantSelectionManager;

    private TenantSelectionFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        filter = new TenantSelectionFilter(tenantSelectionManager);
        request = new MockHttpServletRequest("GET", "/services/web/home/index.html");
        request.setSession(new MockHttpSession());
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
    }

    @Test
    void theOnlyTenantOfAUserIsEnteredWithoutAsking() throws Exception {
        authenticate();
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(assignments(Map.of(ACME, Set.of("Owner")), Set.of()));

        filter.doFilter(request, response, chain);

        verify(tenantSelectionManager).selectTenant(any(), any(), eq(ACME));
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aBrowserWithSeveralTenantsIsSentToThePicker() throws Exception {
        authenticate();
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(
                assignments(Map.of(ACME, Set.of("Owner"), GLOBEX, Set.of("User")), Set.of()));

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo(TenantSelectionFilter.TENANT_SELECTION_PAGE);
        assertThat(chain.getRequest()).isNull();
        verify(tenantSelectionManager, never()).selectTenant(any(), any(), any());
    }

    @Test
    void aProgrammaticCallerWithSeveralTenantsIsToldToChoose() throws Exception {
        authenticate();
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(
                assignments(Map.of(ACME, Set.of("Owner"), GLOBEX, Set.of("User")), Set.of()));
        when(tenantSelectionManager.availableTenants(any())).thenReturn(
                List.of(new TenantOption(ACME, "Acme Ltd", true), new TenantOption(GLOBEX, "Globex", true)));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("TENANT_SELECTION_REQUIRED")
                                                 .contains(ACME)
                                                 .contains(GLOBEX);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void aRequestWithoutAnAcceptHeaderIsTreatedAsProgrammatic() throws Exception {
        authenticate();
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(
                assignments(Map.of(ACME, Set.of("Owner"), GLOBEX, Set.of("User")), Set.of()));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void staffWithoutATenantPassesThrough() throws Exception {
        authenticate();
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(assignments(Map.of(), Set.of("DEVELOPER")));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void aUserWithNeitherTenantsNorGlobalRolesIsRefused() throws Exception {
        authenticate();
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(assignments(Map.of(), Set.of()));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).contains("not assigned to any tenant");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void anExistingSelectionIsKeptConsistent() throws Exception {
        authenticate();
        when(tenantSelectionManager.selectedTenantId(request)).thenReturn(ACME);

        filter.doFilter(request, response, chain);

        verify(tenantSelectionManager).ensureConsistent(request, response);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void theOnlyTenantOfAUserThatCannotBeEnteredSendsThemToThePicker() throws Exception {
        authenticate();
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(assignments(Map.of(ACME, Set.of("Owner")), Set.of()));
        doThrow(new TenantSelectionException(TenantSelectionException.Reason.NOT_PROVISIONED_HERE, ACME,
                "not provisioned yet")).when(tenantSelectionManager)
                                       .selectTenant(any(), any(), eq(ACME));

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo(TenantSelectionFilter.TENANT_SELECTION_PAGE);
    }

    @Test
    void aRequestThatIsNotAnInteractiveSessionPassesThrough() throws Exception {
        // No authentication at all - a machine-to-machine bearer token or an anonymous request.
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantSelectionManager, never()).assignmentsOf(any());
    }

    @Test
    void thePickerAndWhatItLoadsAreNotFiltered() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", TenantSelectionFilter.TENANT_SELECTION_PAGE))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/services/security/tenant-selection"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/webjars/codbex__harmonia/dist/harmonia.css"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/js/platform-branding/branding.js"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/logout"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/web/home/index.html"))).isFalse();
    }

    @Test
    void theFilterIsInertWhereTenantsAreNotSelected() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        TenantSelectionFilter subdomainFilter = new TenantSelectionFilter(tenantSelectionManager);

        assertThat(subdomainFilter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/web/home/index.html"))).isTrue();
    }

    private void authenticate() {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", "owner@example.com"));
        OidcUser oidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak"));
        SecurityContextHolder.setContext(securityContext);
    }

    private static UserTenantAssignments assignments(Map<String, Set<String>> tenantRoles, Set<String> globalRoles) {
        return new UserTenantAssignments(tenantRoles, globalRoles);
    }
}
