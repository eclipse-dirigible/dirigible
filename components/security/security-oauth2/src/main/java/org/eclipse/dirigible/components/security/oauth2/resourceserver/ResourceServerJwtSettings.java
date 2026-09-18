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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * The rules bearer tokens are held to on a login profile: what the profile knows about its identity
 * provider, overridden by the {@code DIRIGIBLE_OAUTH2_JWT_*} configuration.
 *
 * <p>
 * The two token kinds are held to different rules, on purpose. An ID token is what an external
 * frontend sends to act as a user, so it is verified like a login: audience, principal claim, the
 * groups it carries. An access token is what a machine client has always sent, so its rules are the
 * ones that applied before this class existed - issuer and signature - plus an audience check only
 * where the deployment configures audiences. No token accepted before is refused without a
 * configuration change; roles derived from groups are never granted without an audience check.
 *
 * @param provider the identity provider the tokens come from
 * @param jwkSetUri the JWKS endpoint the signatures are verified against
 * @param issuer the issuer every token must carry
 * @param idTokenAudiences the audiences an ID token must name one of
 * @param accessTokenAudiences the audiences an access token must name one of; empty means unchecked
 * @param principalClaim the claim of an ID token the user name is read from
 * @param requireVerifiedEmail whether an ID token identified by its e-mail must carry a verified
 *        one
 * @param acceptedKinds the kinds of tokens accepted at all
 * @param providerGroupsClaim the claim the provider puts the user groups in
 * @param defaultRegistrationId the client registration a token-minted session is filed under
 */
public record ResourceServerJwtSettings(IdentityProvider provider, String jwkSetUri, String issuer, Set<String> idTokenAudiences,
        Set<String> accessTokenAudiences, String principalClaim, boolean requireVerifiedEmail, Set<TokenKind> acceptedKinds,
        String providerGroupsClaim, String defaultRegistrationId) {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceServerJwtSettings.class);

    /** The claim AWS Cognito puts the user groups in. */
    static final String COGNITO_GROUPS_CLAIM = "cognito:groups";

    /** The claim a Keycloak realm typically puts the user groups in. */
    static final String KEYCLOAK_GROUPS_CLAIM = "groups";

    /** The registration id the default tenant's Cognito client is registered under. */
    static final String COGNITO_REGISTRATION_ID = "cognito";

    /** The registration id the default tenant's Keycloak client is registered under. */
    static final String KEYCLOAK_REGISTRATION_ID = "keycloak";

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    /**
     * Keeps the collections immutable whatever the caller handed in.
     */
    public ResourceServerJwtSettings {
        idTokenAudiences = Set.copyOf(idTokenAudiences);
        accessTokenAudiences = Set.copyOf(accessTokenAudiences);
        acceptedKinds = Set.copyOf(acceptedKinds);
    }

    /**
     * The settings of the Cognito profile.
     *
     * @param jwkSetUri the JWKS endpoint of the user pool
     * @param issuerUri the issuer of the user pool
     * @param clientId the app client id of the platform
     * @param userNameAttribute the user-name attribute of the login
     * @return the settings, with the configuration overrides applied
     * @throws InvalidConfigException when the configuration cannot be applied
     */
    public static ResourceServerJwtSettings cognito(String jwkSetUri, String issuerUri, String clientId, String userNameAttribute) {
        return of(IdentityProvider.COGNITO, jwkSetUri, issuerUri, clientId, userNameAttribute, COGNITO_GROUPS_CLAIM,
                COGNITO_REGISTRATION_ID);
    }

    /**
     * The settings of the Keycloak profile.
     *
     * @param jwkSetUri the JWKS endpoint of the realm
     * @param issuerUri the issuer of the realm
     * @param clientId the client id of the platform
     * @param userNameAttribute the user-name attribute of the login
     * @return the settings, with the configuration overrides applied
     * @throws InvalidConfigException when the configuration cannot be applied
     */
    public static ResourceServerJwtSettings keycloak(String jwkSetUri, String issuerUri, String clientId, String userNameAttribute) {
        return of(IdentityProvider.KEYCLOAK, jwkSetUri, issuerUri, clientId, userNameAttribute, KEYCLOAK_GROUPS_CLAIM,
                KEYCLOAK_REGISTRATION_ID);
    }

    private static ResourceServerJwtSettings of(IdentityProvider provider, String profileJwkSetUri, String profileIssuer, String clientId,
            String userNameAttribute, String providerGroupsClaim, String defaultRegistrationId) {
        String jwkSetUri = firstNonBlank(DirigibleConfig.OAUTH2_JWT_JWK_SET_URI.getStringValue(), profileJwkSetUri);
        String issuer = firstNonBlank(DirigibleConfig.OAUTH2_JWT_ISSUER_URI.getStringValue(), profileIssuer);
        String principalClaim = firstNonBlank(DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM.getStringValue(), userNameAttribute);
        requireText(jwkSetUri, DirigibleConfig.OAUTH2_JWT_JWK_SET_URI, "The JWKS endpoint of the bearer tokens is unknown");
        requireText(issuer, DirigibleConfig.OAUTH2_JWT_ISSUER_URI, "The issuer of the bearer tokens is unknown");
        requireText(principalClaim, DirigibleConfig.OAUTH2_JWT_PRINCIPAL_CLAIM,
                "The claim naming the user of a bearer ID token is unknown");

        boolean audiencesConfigured = DirigibleConfig.OAUTH2_JWT_AUDIENCES.isSet();
        List<String> configuredAudiences = DirigibleConfig.OAUTH2_JWT_AUDIENCES.getListValue();
        if (audiencesConfigured && configuredAudiences.isEmpty()) {
            throw new InvalidConfigException("The bearer token audiences are set but name no audience",
                    DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey());
        }
        Set<String> idTokenAudiences = configuredAudiences.isEmpty() ? audienceOf(clientId) : Set.copyOf(configuredAudiences);
        if (idTokenAudiences.isEmpty()) {
            throw new InvalidConfigException(
                    "The audience of bearer ID tokens is unknown: the login profile has no client id and ["
                            + DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey() + "] names none",
                    DirigibleConfig.OAUTH2_JWT_AUDIENCES.getKey());
        }
        Set<String> accessTokenAudiences = audiencesConfigured ? Set.copyOf(configuredAudiences) : Set.of();

        Set<TokenKind> acceptedKinds = TokenKind.fromConfiguration(DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.getListValue(),
                DirigibleConfig.OAUTH2_JWT_TOKEN_KINDS.getKey());
        boolean requireVerifiedEmail = DirigibleConfig.OAUTH2_JWT_REQUIRE_VERIFIED_EMAIL.getBooleanValue();

        warnAboutPlainHttp(jwkSetUri, "JWKS endpoint");
        warnAboutPlainHttp(issuer, "issuer");
        LOGGER.info(
                "Bearer tokens of [{}] are accepted: kinds {}, issuer [{}], ID token audiences {}, access token audiences {},"
                        + " principal claim of ID tokens [{}]",
                provider, acceptedKinds, issuer, idTokenAudiences, accessTokenAudiences.isEmpty() ? "unchecked" : accessTokenAudiences,
                principalClaim);

        return new ResourceServerJwtSettings(provider, jwkSetUri, issuer, idTokenAudiences, accessTokenAudiences, principalClaim,
                requireVerifiedEmail, acceptedKinds, providerGroupsClaim, defaultRegistrationId);
    }

    /**
     * Reads the kind of a token the way this provider marks it.
     *
     * @param jwt the decoded token
     * @return the kind, empty for a token of no accepted kind
     */
    public Optional<TokenKind> kindOf(Jwt jwt) {
        return TokenKind.of(jwt, provider);
    }

    private static String firstNonBlank(String configured, String fallback) {
        return StringUtils.hasText(configured) ? configured.trim() : fallback;
    }

    private static void requireText(String value, DirigibleConfig config, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidConfigException(message + ": neither the login profile nor [" + config.getKey() + "] provides it",
                    config.getKey());
        }
    }

    private static Set<String> audienceOf(String clientId) {
        return StringUtils.hasText(clientId) ? Set.of(clientId.trim()) : Set.of();
    }

    private static void warnAboutPlainHttp(String uri, String what) {
        URI parsed;
        try {
            parsed = new URI(uri);
        } catch (URISyntaxException ex) {
            LOGGER.warn("The {} [{}] of the bearer tokens is not a valid URI", what, uri, ex);
            return;
        }
        String scheme = parsed.getScheme() == null ? ""
                : parsed.getScheme()
                        .toLowerCase(Locale.ROOT);
        String host = parsed.getHost() == null ? ""
                : parsed.getHost()
                        .toLowerCase(Locale.ROOT);
        if ("http".equals(scheme) && !LOCAL_HOSTS.contains(host)) {
            LOGGER.warn("The {} [{}] of the bearer tokens is reached over plain HTTP. Anyone on the path can substitute the keys"
                    + " the tokens are verified against.", what, uri);
        }
    }
}
