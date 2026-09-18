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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The kinds of bearer tokens the platform tells apart, because they prove different things and are
 * held to different rules: an ID token identifies a user, an access token is the grant of a client.
 * Anything else an identity provider issues - a refresh token, an offline token - is never accepted
 * as a bearer token, and neither is a token that does not say what it is.
 */
public enum TokenKind {

    /**
     * An ID token: the identity of a user, named by the principal claim and holding the user's groups.
     */
    ID("id"),

    /**
     * An access token: the grant of a client, typically a machine client, identified by {@code sub}.
     */
    ACCESS("access");

    /** The claim AWS Cognito marks the kind in. */
    static final String COGNITO_TOKEN_USE_CLAIM = "token_use";

    /** The claim Keycloak marks the kind in. */
    static final String KEYCLOAK_TYPE_CLAIM = "typ";

    private final String configurationName;

    TokenKind(String configurationName) {
        this.configurationName = configurationName;
    }

    /**
     * Gets the name the kind goes by in the configuration.
     *
     * @return the configuration name
     */
    public String getConfigurationName() {
        return configurationName;
    }

    /**
     * Reads the kind of a token the way its issuer marks it.
     *
     * @param jwt the decoded token
     * @param provider the issuer of the token
     * @return the kind, empty for a token of no accepted kind
     */
    public static Optional<TokenKind> of(Jwt jwt, IdentityProvider provider) {
        return switch (provider) {
            case COGNITO -> ofCognito(jwt.getClaimAsString(COGNITO_TOKEN_USE_CLAIM));
            case KEYCLOAK -> ofKeycloak(jwt.getClaimAsString(KEYCLOAK_TYPE_CLAIM));
        };
    }

    private static Optional<TokenKind> ofCognito(String tokenUse) {
        if ("id".equals(tokenUse)) {
            return Optional.of(ID);
        }
        if ("access".equals(tokenUse)) {
            return Optional.of(ACCESS);
        }
        return Optional.empty();
    }

    private static Optional<TokenKind> ofKeycloak(String type) {
        // a Keycloak access token is typed Bearer, an ID token ID. Keycloak types every token it
        // issues, so one typed nothing at all is not Keycloak's - typically the ID token of another
        // issuer the profile was pointed at - and is refused rather than read as an access token,
        // which would hand it the weaker rules of that kind
        if ("Bearer".equals(type)) {
            return Optional.of(ACCESS);
        }
        if ("ID".equals(type)) {
            return Optional.of(ID);
        }
        return Optional.empty();
    }

    /**
     * Resolves the kinds a configuration value names.
     *
     * @param names the configured names ({@code id}, {@code access})
     * @param configKey the configuration key, for the error
     * @return the kinds, never empty
     * @throws InvalidConfigException for an unknown name or no name at all
     */
    static Set<TokenKind> fromConfiguration(List<String> names, String configKey) {
        Set<TokenKind> kinds = EnumSet.noneOf(TokenKind.class);
        for (String name : names) {
            kinds.add(Arrays.stream(values())
                            .filter(kind -> kind.configurationName.equalsIgnoreCase(name))
                            .findFirst()
                            .orElseThrow(() -> new InvalidConfigException(
                                    "Unknown bearer token kind [" + name + "], supported are [id, access]", configKey)));
        }
        if (kinds.isEmpty()) {
            throw new InvalidConfigException("At least one bearer token kind must be accepted", configKey);
        }
        return Collections.unmodifiableSet(kinds);
    }
}
