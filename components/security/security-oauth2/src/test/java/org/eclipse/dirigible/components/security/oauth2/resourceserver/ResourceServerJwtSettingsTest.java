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

import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The profile supplies what it knows about its identity provider, the configuration overrides it,
 * and a configuration that leaves a rule undefined is refused rather than guessed.
 */
class ResourceServerJwtSettingsTest {

    private static final String JWKS = "https://idp.example.org/.well-known/jwks.json";
    private static final String ISSUER = "https://idp.example.org";
    private static final String CLIENT_ID = "the-client";

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_ISSUER_URI.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_JWK_SET_URI.getKey());
        Configuration.remove(DirigibleConfig.OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL.getKey());
    }

    @Test
    void cognitoDefaultsComeFromTheProfile() {
        ResourceServerJwtSettings settings = ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, "email");

        assertEquals(IdentityProvider.COGNITO, settings.provider());
        assertEquals(JWKS, settings.jwkSetUri());
        assertEquals(ISSUER, settings.issuer());
        assertEquals(Set.of(CLIENT_ID), settings.idTokenAudiences());
        assertTrue(settings.accessTokenAudiences()
                           .isEmpty(),
                "access tokens accepted so far carry no audience - they stay unchecked unless audiences are configured");
        assertEquals("email", settings.principalClaim());
        assertTrue(settings.requireVerifiedEmail());
        assertEquals(Set.of(TokenKind.ID, TokenKind.ACCESS), settings.acceptedKinds());
        assertEquals("cognito:groups", settings.providerGroupsClaim());
        assertEquals("cognito", settings.defaultRegistrationId());
    }

    @Test
    void keycloakDefaultsComeFromTheProfile() {
        ResourceServerJwtSettings settings = ResourceServerJwtSettings.keycloak(JWKS, ISSUER, CLIENT_ID, "preferred_username");

        assertEquals(IdentityProvider.KEYCLOAK, settings.provider());
        assertEquals("preferred_username", settings.principalClaim());
        assertEquals("groups", settings.providerGroupsClaim());
        assertEquals("keycloak", settings.defaultRegistrationId());
    }

    @Test
    void theConfigurationOverridesTheProfile() {
        DirigibleConfig.OAUTH2_JWT_JWK_SET_URI.setStringValue("https://other.example.org/jwks");
        DirigibleConfig.OAUTH2_JWT_ISSUER_URI.setStringValue("https://other.example.org");
        DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.setStringValue("cognito:username");
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(" api://platform , the-client ");
        DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.setStringValue("id");
        DirigibleConfig.OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL.setBooleanValue(false);

        ResourceServerJwtSettings settings = ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, "email");

        assertEquals("https://other.example.org/jwks", settings.jwkSetUri());
        assertEquals("https://other.example.org", settings.issuer());
        assertEquals("cognito:username", settings.principalClaim());
        assertEquals(Set.of("api://platform", "the-client"), settings.idTokenAudiences());
        assertEquals(Set.of("api://platform", "the-client"), settings.accessTokenAudiences());
        assertEquals(Set.of(TokenKind.ID), settings.acceptedKinds());
        assertFalse(settings.requireVerifiedEmail());
    }

    @Test
    void anUnknownJwksEndpointIsRefused() {
        InvalidConfigException exception =
                assertThrows(InvalidConfigException.class, () -> ResourceServerJwtSettings.cognito(" ", ISSUER, CLIENT_ID, "email"));

        assertEquals(DirigibleConfig.OAUTH2_JWT_JWK_SET_URI.getKey(), exception.getConfigKey());
    }

    @Test
    void anUnknownIssuerIsRefused() {
        InvalidConfigException exception = assertThrows(InvalidConfigException.class,
                () -> ResourceServerJwtSettings.keycloak(JWKS, null, CLIENT_ID, "preferred_username"));

        assertEquals(DirigibleConfig.OAUTH2_JWT_ISSUER_URI.getKey(), exception.getConfigKey());
    }

    @Test
    void anUnknownPrincipalClaimIsRefused() {
        InvalidConfigException exception =
                assertThrows(InvalidConfigException.class, () -> ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, ""));

        assertEquals(DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.getKey(), exception.getConfigKey());
    }

    @Test
    void anUnknownIdTokenAudienceIsRefused() {
        InvalidConfigException exception =
                assertThrows(InvalidConfigException.class, () -> ResourceServerJwtSettings.cognito(JWKS, ISSUER, " ", "email"));

        assertEquals(DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey(), exception.getConfigKey());
    }

    @Test
    void configuredAudiencesReplaceTheClientId() {
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue("api://platform");

        ResourceServerJwtSettings settings = ResourceServerJwtSettings.cognito(JWKS, ISSUER, " ", "email");

        assertEquals(Set.of("api://platform"), settings.idTokenAudiences());
        assertEquals(Set.of("api://platform"), settings.accessTokenAudiences());
    }

    @Test
    void audiencesSetToNothingAreRefused() {
        DirigibleConfig.OAUTH2_JWT_AUDIENCES.setStringValue(" , ");

        InvalidConfigException exception =
                assertThrows(InvalidConfigException.class, () -> ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, "email"));

        assertEquals(DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey(), exception.getConfigKey());
    }

    @Test
    void unknownTokenKindsAreRefused() {
        DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.setStringValue("id,refresh");

        InvalidConfigException exception =
                assertThrows(InvalidConfigException.class, () -> ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, "email"));

        assertEquals(DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.getKey(), exception.getConfigKey());
    }

    @Test
    void theSettingsAreImmutable() {
        ResourceServerJwtSettings settings = ResourceServerJwtSettings.cognito(JWKS, ISSUER, CLIENT_ID, "email");

        assertThrows(UnsupportedOperationException.class, () -> settings.idTokenAudiences()
                                                                        .add("x"));
        assertThrows(UnsupportedOperationException.class, () -> settings.acceptedKinds()
                                                                        .remove(TokenKind.ID));
    }
}
