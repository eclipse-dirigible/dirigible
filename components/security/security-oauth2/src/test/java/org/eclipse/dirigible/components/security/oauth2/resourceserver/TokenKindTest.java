/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.resourceserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Cognito marks the kind in {@code token_use}, Keycloak in {@code typ}; anything that is neither an
 * ID nor an access token has no kind and is never accepted.
 */
class TokenKindTest {

    @Test
    void cognitoReadsTokenUse() {
        assertEquals(Optional.of(TokenKind.ID), TokenKind.of(jwt("token_use", "id"), IdentityProvider.COGNITO));
        assertEquals(Optional.of(TokenKind.ACCESS), TokenKind.of(jwt("token_use", "access"), IdentityProvider.COGNITO));
        assertEquals(Optional.empty(), TokenKind.of(jwt("token_use", "refresh"), IdentityProvider.COGNITO));
        assertEquals(Optional.empty(), TokenKind.of(jwt("token_use", "ID"), IdentityProvider.COGNITO));
        assertEquals(Optional.empty(), TokenKind.of(jwt("typ", "ID"), IdentityProvider.COGNITO),
                "a Cognito token without token_use has no kind");
    }

    @Test
    void keycloakReadsTyp() {
        assertEquals(Optional.of(TokenKind.ID), TokenKind.of(jwt("typ", "ID"), IdentityProvider.KEYCLOAK));
        assertEquals(Optional.of(TokenKind.ACCESS), TokenKind.of(jwt("typ", "Bearer"), IdentityProvider.KEYCLOAK));
        assertEquals(Optional.empty(), TokenKind.of(jwt("scope", "openid"), IdentityProvider.KEYCLOAK),
                "Keycloak types every token it issues, so one typed nothing is not Keycloak's and has no kind here");
        assertEquals(Optional.empty(), TokenKind.of(jwt("typ", "Refresh"), IdentityProvider.KEYCLOAK));
        assertEquals(Optional.empty(), TokenKind.of(jwt("typ", "Offline"), IdentityProvider.KEYCLOAK));
        assertEquals(Optional.empty(), TokenKind.of(jwt("typ", "id"), IdentityProvider.KEYCLOAK));
    }

    @Test
    void configurationNamesResolveCaseInsensitively() {
        assertEquals(Set.of(TokenKind.ID, TokenKind.ACCESS), TokenKind.fromConfiguration(List.of("id", "access"), "KEY"));
        assertEquals(Set.of(TokenKind.ID), TokenKind.fromConfiguration(List.of("ID"), "KEY"));
        assertEquals(Set.of(TokenKind.ACCESS), TokenKind.fromConfiguration(List.of("Access"), "KEY"));
    }

    @Test
    void anUnknownOrMissingKindIsRefused() {
        InvalidConfigException unknown =
                assertThrows(InvalidConfigException.class, () -> TokenKind.fromConfiguration(List.of("id", "refresh"), "KEY"));
        assertEquals("KEY", unknown.getConfigKey());
        assertTrue(unknown.getMessage()
                          .contains("refresh"));

        InvalidConfigException none = assertThrows(InvalidConfigException.class, () -> TokenKind.fromConfiguration(List.of(), "KEY"));
        assertEquals("KEY", none.getConfigKey());
    }

    private static Jwt jwt(String claim, String value) {
        return Jwt.withTokenValue("token")
                  .header("alg", "RS256")
                  .subject("subject")
                  .claim(claim, value)
                  .issuedAt(Instant.now())
                  .expiresAt(Instant.now()
                                    .plusSeconds(300))
                  .build();
    }
}
