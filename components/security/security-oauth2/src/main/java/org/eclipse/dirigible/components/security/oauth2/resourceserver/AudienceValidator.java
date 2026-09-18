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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Requires a token to be issued for this deployment: the audiences it presents must include one of
 * the configured ones. Without the check a token minted for any other application of the same user
 * pool or realm passes signature and issuer validation and, with the group roles an ID token now
 * grants, would act here with the roles of its user.
 */
final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    /**
     * The claim a Cognito access token names its client in - Cognito access tokens carry no
     * {@code aud}.
     */
    static final String COGNITO_CLIENT_ID_CLAIM = "client_id";

    private static final OAuth2Error NOT_INTENDED_FOR_THIS_CLIENT = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "The token is not intended for this client", "https://tools.ietf.org/html/rfc6750#section-3.1");

    private final Set<String> audiences;
    private final Function<Jwt, Collection<String>> presentedAudiences;

    /**
     * Instantiates the validator.
     *
     * @param audiences the accepted audiences, at least one
     * @param presentedAudiences how the token presents its audiences
     */
    AudienceValidator(Set<String> audiences, Function<Jwt, Collection<String>> presentedAudiences) {
        if (audiences.isEmpty()) {
            throw new IllegalArgumentException("At least one audience is required");
        }
        this.audiences = Set.copyOf(audiences);
        this.presentedAudiences = presentedAudiences;
    }

    /**
     * The validator of tokens presenting their audiences in the standard {@code aud} claim.
     *
     * @param audiences the accepted audiences
     * @return the validator
     */
    static AudienceValidator ofAudienceClaim(Set<String> audiences) {
        return new AudienceValidator(audiences, Jwt::getAudience);
    }

    /**
     * The validator of Cognito access tokens, which name their client in {@code client_id}.
     *
     * @param audiences the accepted audiences
     * @return the validator
     */
    static AudienceValidator ofCognitoClientId(Set<String> audiences) {
        return new AudienceValidator(audiences, jwt -> {
            String clientId = jwt.getClaimAsString(COGNITO_CLIENT_ID_CLAIM);
            return clientId == null ? List.of() : List.of(clientId);
        });
    }

    /**
     * Validate.
     *
     * @param jwt the token
     * @return success when the token presents an accepted audience
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Collection<String> presented = presentedAudiences.apply(jwt);
        if (presented != null && presented.stream()
                                          .anyMatch(audiences::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(NOT_INTENDED_FOR_THIS_CLIENT);
    }
}
