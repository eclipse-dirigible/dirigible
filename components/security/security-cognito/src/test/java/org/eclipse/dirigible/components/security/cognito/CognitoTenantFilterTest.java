/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.tenant.TenantExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * In the shared user pool mode a user must belong to the tenant of the host. A bearer ID token is a
 * user and is held to it; an access token of a machine client carries no tenant and passes as it
 * always did, unless it names tenants.
 */
class CognitoTenantFilterTest {

    private TenantExtractor tenantExtractor;
    private CognitoTenantFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        DirigibleConfig.MULTI_TENANT_MODE_ENABLED.setBooleanValue(true);
        DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.setBooleanValue(true);
        Tenant acme = mock(Tenant.class);
        when(acme.getSubdomain()).thenReturn("acme");
        when(acme.getName()).thenReturn("Acme");
        tenantExtractor = mock(TenantExtractor.class);
        when(tenantExtractor.determineTenantSubdomain(any())).thenReturn(Optional.of(acme));
        filter = new CognitoTenantFilter(tenantExtractor);
        request = new MockHttpServletRequest("GET", "/services/ide/workspaces");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getKey());
        Configuration.remove(DirigibleConfig.MULTI_TENANT_MODE_COGNITO_SINGLE_USER_POOL_ENABLED.getKey());
    }

    @Test
    void aLoggedInMemberPasses() throws Exception {
        request.setUserPrincipal(login("acme,globex"));

        filter.doFilter(request, response, chain);

        assertPassed();
    }

    @Test
    void anIdTokenOfAMemberPasses() throws Exception {
        request.setUserPrincipal(bearer("id", "globex, acme"));

        filter.doFilter(request, response, chain);

        assertPassed();
    }

    @Test
    void anIdTokenOfANonMemberIsForbidden() throws Exception {
        request.setUserPrincipal(bearer("id", "globex"));

        filter.doFilter(request, response, chain);

        assertForbidden();
    }

    @Test
    void anIdTokenWithoutTenantsIsForbidden() throws Exception {
        request.setUserPrincipal(bearer("id", null));

        filter.doFilter(request, response, chain);

        assertForbidden();
    }

    @Test
    void anAccessTokenWithoutTenantsPassesAsBefore() throws Exception {
        request.setUserPrincipal(bearer("access", null));

        filter.doFilter(request, response, chain);

        assertPassed();
    }

    @Test
    void anAccessTokenNamingOtherTenantsIsForbidden() throws Exception {
        request.setUserPrincipal(bearer("access", "globex"));

        filter.doFilter(request, response, chain);

        assertForbidden();
    }

    @Test
    void anAnonymousRequestPasses() throws Exception {
        filter.doFilter(request, response, chain);

        assertPassed();
    }

    private void assertPassed() {
        assertNotNull(chain.getRequest(), "the chain must continue");
        assertEquals(200, response.getStatus());
    }

    private void assertForbidden() {
        assertNull(chain.getRequest(), "the chain must not continue");
        assertEquals(403, response.getStatus());
    }

    private static OAuth2AuthenticationToken login(String tenants) {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", "jane", "custom:tenant", tenants));
        DefaultOidcUser user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")), idToken);
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "cognito");
    }

    private static JwtAuthenticationToken bearer(String tokenUse, String tenants) {
        Jwt.Builder jwt = Jwt.withTokenValue("bearer-token")
                             .header("alg", "RS256")
                             .subject("jane")
                             .claim("token_use", tokenUse)
                             .issuedAt(Instant.now())
                             .expiresAt(Instant.now()
                                               .plusSeconds(300));
        if (tenants != null) {
            jwt.claim("custom:tenant", tenants);
        }
        return new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")), "jane");
    }
}
