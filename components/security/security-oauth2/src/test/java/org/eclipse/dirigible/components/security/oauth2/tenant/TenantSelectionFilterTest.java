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
import static org.mockito.ArgumentMatchers.anyString;
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
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.groups.UserTenantAssignments;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSettings;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSupport;
import org.eclipse.dirigible.components.tenants.tenant.TenantSelectionConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The decision the filter makes on every request. For an interactive session: one tenant is entered
 * silently, several send the user to the picker (a browser by redirect, a programmatic caller by a
 * conflict), none is fine for staff and refused for everyone else, and a selection that already
 * exists is kept consistent. For a bearer request: the tenant named in the header is entered for
 * that request, one that cannot be entered is refused with its reason, and a request naming none
 * stays in the default tenant - unless an ID token holds no global role, which is refused.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSelectionFilterTest {

    private static final String ACME = "acme";
    private static final String GLOBEX = "globex";

    @Mock
    private TenantSelectionManager tenantSelectionManager;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ObjectProvider<ResourceServerJwtSupport> bearerTokenSupport;

    @Mock
    private ResourceServerJwtSupport resourceServerJwtSupport;

    @Mock
    private Tenant acmeTenant;

    private TenantSelectionFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        DirigibleConfig.TENANT_RESOLUTION_STRATEGY.setStringValue("TOKEN_GROUPS");
        filter = new TenantSelectionFilter(tenantSelectionManager, tenantContext, bearerTokenSupport);
        // The real one opens the tenant scope around the callable; here it just runs it.
        try {
            when(tenantContext.execute(anyString(), any(CallableResultAndException.class))).thenAnswer(
                    invocation -> ((CallableResultAndException<?, ?>) invocation.getArgument(1)).call());
            when(tenantContext.execute(any(Tenant.class), any(CallableResultAndException.class))).thenAnswer(
                    invocation -> ((CallableResultAndException<?, ?>) invocation.getArgument(1)).call());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        // The Keycloak rules: a token typed ID identifies a user, one typed Bearer is an access token.
        when(bearerTokenSupport.getIfAvailable()).thenReturn(resourceServerJwtSupport);
        when(resourceServerJwtSupport.settings()).thenReturn(ResourceServerJwtSettings.keycloak("http://localhost/certs",
                "http://localhost/realms/it", "the-client", "preferred_username"));
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

    /**
     * The regression this guards: the tenant scope of the current request was opened before the
     * selection existed, so without re-entering it the request is served with the roles of the selected
     * tenant and the data of the default one.
     */
    @Test
    void theRequestThatAutoSelectedIsAlreadyServedInThatTenant() throws Exception {
        authenticate();
        when(tenantSelectionManager.assignmentsOf(any())).thenReturn(assignments(Map.of(ACME, Set.of("Owner")), Set.of()));

        filter.doFilter(request, response, chain);

        verify(tenantContext).execute(eq(ACME), any(CallableResultAndException.class));
        assertThat(chain.getRequest()).isNotNull();
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
                List.of(new TenantOption(ACME, "Acme Ltd", true, TenantOption.State.READY),
                        new TenantOption(GLOBEX, "Globex", true, TenantOption.State.READY)));

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
    void aRequestThatIsNeitherASessionNorABearerPassesThrough() throws Exception {
        // No authentication at all - an anonymous request.
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantSelectionManager, never()).assignmentsOf(any());
    }

    // --- bearer requests -----------------------------------------------------------------------

    @Test
    void aBearerRequestNamingATenantRunsInItWithTheRolesOfThatTenant() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID", "DEVELOPER");
        request.addHeader(TenantSelectionConstants.TENANT_HEADER, ACME);
        JwtAuthenticationToken entered =
                new JwtAuthenticationToken(bearer.getToken(), AuthoritiesUtil.toAuthorities("DEVELOPER", "Owner"), bearer.getName());
        when(tenantSelectionManager.enterTenant(bearer, ACME)).thenReturn(new BearerTenantSelection(acmeTenant, entered));

        filter.doFilter(request, response, chain);

        verify(tenantContext).execute(eq(acmeTenant), any(CallableResultAndException.class));
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext()
                                        .getAuthentication()).isSameAs(entered);
    }

    @Test
    void aBearerRequestNamingATenantItCannotEnterIsRefusedWithTheReason() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID", "DEVELOPER");
        request.addHeader(TenantSelectionConstants.TENANT_HEADER, GLOBEX);
        doThrow(new TenantSelectionException(TenantSelectionException.Reason.NOT_A_MEMBER, GLOBEX, "not a member")).when(
                tenantSelectionManager)
                                                                                                                   .enterTenant(bearer,
                                                                                                                           GLOBEX);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("NOT_A_MEMBER")
                                                 .contains("not a member");
        assertThat(chain.getRequest()).isNull();
        verify(tenantContext, never()).execute(any(Tenant.class), any(CallableResultAndException.class));
        assertThat(SecurityContextHolder.getContext()
                                        .getAuthentication()).isSameAs(bearer);
    }

    @Test
    void aBearerRequestNamingATenantNotProvisionedYetIsAnsweredWithAConflict() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID", "DEVELOPER");
        request.addHeader(TenantSelectionConstants.TENANT_HEADER, ACME);
        doThrow(new TenantSelectionException(TenantSelectionException.Reason.NOT_PROVISIONED_HERE, ACME,
                "not provisioned yet")).when(tenantSelectionManager)
                                       .enterTenant(bearer, ACME);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("NOT_PROVISIONED_HERE");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void aBearerRequestNamingNoTenantStaysInTheDefaultTenantWithItsGlobalRoles() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID", "DEVELOPER");
        when(tenantSelectionManager.assignmentsOf(bearer)).thenReturn(assignments(Map.of(ACME, Set.of("Owner")), Set.of("DEVELOPER")));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(tenantSelectionManager, never()).enterTenant(any(), any());
        verify(tenantContext, never()).execute(any(Tenant.class), any(CallableResultAndException.class));
        assertThat(SecurityContextHolder.getContext()
                                        .getAuthentication()).isSameAs(bearer);
    }

    @Test
    void anIdTokenWithoutAGlobalRoleHasToNameATenant() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID");
        when(tenantSelectionManager.assignmentsOf(bearer)).thenReturn(assignments(Map.of(ACME, Set.of("Owner")), Set.of()));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).contains(TenantSelectionConstants.TENANT_HEADER);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void aBlankTenantHeaderCountsAsNone() throws Exception {
        JwtAuthenticationToken bearer = authenticateBearer("ID", "DEVELOPER");
        request.addHeader(TenantSelectionConstants.TENANT_HEADER, "   ");
        when(tenantSelectionManager.assignmentsOf(bearer)).thenReturn(assignments(Map.of(), Set.of("DEVELOPER")));

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantSelectionManager, never()).enterTenant(any(), any());
    }

    @Test
    void anAccessTokenNamingNoTenantPassesAsItAlwaysDid() throws Exception {
        // A machine client's token carries no groups, so it cannot be held to having a tenant.
        authenticateBearer("Bearer");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(tenantSelectionManager, never()).assignmentsOf(any());
    }

    @Test
    void onAProfileWithoutBearerSupportATokenNamingNoTenantPassesThrough() throws Exception {
        when(bearerTokenSupport.getIfAvailable()).thenReturn(null);
        authenticateBearer("ID");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(tenantSelectionManager, never()).assignmentsOf(any());
    }

    @Test
    void thePickerAndWhatItLoadsAreNotFiltered() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", TenantSelectionFilter.TENANT_SELECTION_PAGE))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/services/security/tenant-selection"))).isTrue();
        // The current-tenant endpoint sits under the selection endpoint's path on purpose: a user who has
        // not chosen yet is told they are in the default tenant, not answered with the 409.
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/security/tenant-selection/current"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/webjars/codbex__harmonia/dist/harmonia.css"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/js/platform-branding/branding.js"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/logout"))).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/services/web/home/index.html"))).isFalse();
    }

    @Test
    void theFilterIsInertWhereTenantsAreNotSelected() {
        Configuration.remove(DirigibleConfig.TENANT_RESOLUTION_STRATEGY.getKey());
        TenantSelectionFilter subdomainFilter = new TenantSelectionFilter(tenantSelectionManager, tenantContext, bearerTokenSupport);

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

    /**
     * An authenticated bearer token of the given Keycloak type, carrying the roles the chain granted it
     * - the global ones, under the token groups strategy.
     */
    private JwtAuthenticationToken authenticateBearer(String type, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                     .header("alg", "RS256")
                     .subject("sub-jane")
                     .claim("typ", type)
                     .claim("preferred_username", "jane")
                     .issuedAt(Instant.now())
                     .expiresAt(Instant.now()
                                       .plusSeconds(300))
                     .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, AuthoritiesUtil.toAuthorities(roles), "jane");
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        return authentication;
    }

    private static UserTenantAssignments assignments(Map<String, Set<String>> tenantRoles, Set<String> globalRoles) {
        return new UserTenantAssignments(tenantRoles, globalRoles);
    }
}
