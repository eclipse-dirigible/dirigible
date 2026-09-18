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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * A token passes when the audiences it presents include one of the accepted ones - read from
 * {@code aud}, or from {@code client_id} for a Cognito access token, which carries no {@code aud}.
 */
class AudienceValidatorTest {

    @Test
    void anAudienceClaimNamingAnAcceptedAudiencePasses() {
        AudienceValidator validator = AudienceValidator.ofAudienceClaim(Set.of("the-client", "api://platform"));

        assertFalse(validator.validate(jwt().audience(List.of("other", "api://platform"))
                                            .build())
                             .hasErrors());
    }

    @Test
    void anAudienceClaimNamingNoAcceptedAudienceFailsAsInvalidToken() {
        AudienceValidator validator = AudienceValidator.ofAudienceClaim(Set.of("the-client"));

        OAuth2TokenValidatorResult result = validator.validate(jwt().audience(List.of("other-client"))
                                                                    .build());

        assertTrue(result.hasErrors());
        assertEquals(OAuth2ErrorCodes.INVALID_TOKEN, result.getErrors()
                                                           .iterator()
                                                           .next()
                                                           .getErrorCode());
    }

    @Test
    void aMissingAudienceClaimFails() {
        AudienceValidator validator = AudienceValidator.ofAudienceClaim(Set.of("the-client"));

        assertTrue(validator.validate(jwt().build())
                            .hasErrors());
    }

    @Test
    void aCognitoAccessTokenIsCheckedByItsClientId() {
        AudienceValidator validator = AudienceValidator.ofCognitoClientId(Set.of("the-client"));

        assertFalse(validator.validate(jwt().claim("client_id", "the-client")
                                            .build())
                             .hasErrors());
        assertTrue(validator.validate(jwt().claim("client_id", "other-client")
                                           .build())
                            .hasErrors());
        assertTrue(validator.validate(jwt().audience(List.of("the-client"))
                                           .build())
                            .hasErrors(),
                "a Cognito access token presents its client in client_id, never in aud");
    }

    @Test
    void atLeastOneAudienceIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> AudienceValidator.ofAudienceClaim(Set.of()));
    }

    private static Jwt.Builder jwt() {
        return Jwt.withTokenValue("token")
                  .header("alg", "RS256")
                  .subject("subject")
                  .issuedAt(Instant.now())
                  .expiresAt(Instant.now()
                                    .plusSeconds(300));
    }
}
