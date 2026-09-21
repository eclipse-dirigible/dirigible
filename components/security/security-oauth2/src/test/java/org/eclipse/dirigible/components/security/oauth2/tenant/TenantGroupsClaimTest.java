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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The one reading of the groups claim, whichever way the user arrived: the claims of a login's ID
 * token, the claims of a bearer ID token, nothing for an authentication that carries no claims.
 */
class TenantGroupsClaimTest {

    private static final String GROUPS_CLAIM = "groups";

    private final TenantGroupsClaim claim = new TenantGroupsClaim(GROUPS_CLAIM);

    @Test
    void theGroupsOfALoginAreReadFromItsIdToken() {
        OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now()
                                                                                .plusSeconds(300),
                Map.of("sub", "jane", GROUPS_CLAIM, List.of("acme.library.Owner", "DEVELOPER")));
        OAuth2AuthenticationToken login = new OAuth2AuthenticationToken(
                new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken), List.of(), "keycloak");

        assertThat(claim.groupsOf(login)).containsExactly("acme.library.Owner", "DEVELOPER");
    }

    @Test
    void theGroupsOfABearerTokenAreReadFromItsClaims() {
        JwtAuthenticationToken bearer =
                new JwtAuthenticationToken(jwt(Map.of(GROUPS_CLAIM, List.of("acme.library.Owner", "DEVELOPER"))), List.of(), "jane");

        assertThat(claim.groupsOf(bearer)).containsExactly("acme.library.Owner", "DEVELOPER");
    }

    @Test
    void aBearerTokenWithoutTheClaimHasNoGroups() {
        JwtAuthenticationToken bearer = new JwtAuthenticationToken(jwt(Map.of("scope", "openid")), List.of(), "jane");

        assertThat(claim.groupsOf(bearer)).isEmpty();
    }

    @Test
    void aClaimThatIsNotACollectionYieldsNoGroups() {
        JwtAuthenticationToken bearer = new JwtAuthenticationToken(jwt(Map.of(GROUPS_CLAIM, "acme.library.Owner")), List.of(), "jane");

        assertThat(claim.groupsOf(bearer)).isEmpty();
    }

    @Test
    void anAuthenticationWithoutClaimsHasNoGroups() {
        assertThat(claim.groupsOf(new UsernamePasswordAuthenticationToken("jane", "secret"))).isEmpty();
        assertThat(claim.groupsOf((Authentication) null)).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder jwt = Jwt.withTokenValue("token")
                             .header("alg", "RS256")
                             .subject("sub-jane")
                             .issuedAt(Instant.now())
                             .expiresAt(Instant.now()
                                               .plusSeconds(300));
        claims.forEach(jwt::claim);
        return jwt.build();
    }
}
