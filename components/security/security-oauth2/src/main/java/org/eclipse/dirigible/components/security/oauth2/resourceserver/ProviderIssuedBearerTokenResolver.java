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

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the bearer token of a request only when the token names the identity provider as its
 * issuer. A request carrying any other bearer token - one the application issued itself, a
 * webhook's, a token that is not a JWT at all - is not a bearer request to the platform: it goes on
 * with its session or anonymously, and the application's own controller decides what the token is
 * worth. Left to the resource-server filter, such a token was refused on every path, public ones
 * included, and cost a fetch of the provider's key set on the way.
 *
 * <p>
 * The issuer is read without verifying the signature. It only decides whether the token is the
 * provider's to verify: a token forging the provider's issuer gains nothing but the full
 * verification against the provider's keys.
 */
final class ProviderIssuedBearerTokenResolver implements BearerTokenResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderIssuedBearerTokenResolver.class);

    private final BearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
    private final String issuer;

    /**
     * Instantiates a resolver for the tokens of one provider.
     *
     * @param issuer the issuer the provider's tokens carry
     */
    ProviderIssuedBearerTokenResolver(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String token = headerResolver.resolve(request);
        if (token == null || namesIssuer(token, issuer)) {
            return token;
        }
        LOGGER.debug("The bearer token of a request to [{}] is not issued by [{}], so it is left to the application",
                request.getRequestURI(), issuer);
        return null;
    }

    /**
     * Tells whether a token names an issuer, without verifying it.
     *
     * @param token the token
     * @param issuer the issuer
     * @return whether the token is a JWT whose readable claims carry exactly that issuer
     */
    static boolean namesIssuer(String token, String issuer) {
        try {
            // an encrypted token has no readable claims until it is decrypted
            JWTClaimsSet claims = JWTParser.parse(token)
                                           .getJWTClaimsSet();
            return claims != null && issuer.equals(claims.getIssuer());
        } catch (ParseException ex) {
            LOGGER.debug("A bearer token that is not a JWT names no issuer", ex);
            return false;
        }
    }
}
