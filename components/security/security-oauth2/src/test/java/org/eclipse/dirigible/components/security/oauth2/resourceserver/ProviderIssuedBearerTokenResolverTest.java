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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Only a token naming the provider as its issuer makes a request a bearer request to the platform
 * (#8592): any other bearer token is left to the application. The signature is never verified here,
 * so the tokens are signed with an arbitrary secret.
 */
class ProviderIssuedBearerTokenResolverTest {

    private static final String ISSUER = "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_TEST";

    private final ProviderIssuedBearerTokenResolver resolver = new ProviderIssuedBearerTokenResolver(ISSUER);

    @Test
    void aTokenNamingTheProviderIsResolved() throws Exception {
        String token = token(claims -> claims.issuer(ISSUER));

        assertEquals(token, resolver.resolve(bearer(token)));
    }

    @Test
    void aTokenOfAnotherIssuerIsLeftToTheApplication() throws Exception {
        String token = token(claims -> claims.issuer("https://the-application.example.org/oidc"));

        assertNull(resolver.resolve(bearer(token)));
    }

    @Test
    void aTokenNamingNoIssuerIsLeftToTheApplication() throws Exception {
        assertNull(resolver.resolve(bearer(token(claims -> claims.subject("webhook")))));
    }

    @Test
    void aTokenThatIsNotAJwtIsLeftToTheApplication() {
        assertNull(resolver.resolve(bearer("an-opaque-webhook-secret")));
    }

    @Test
    void aRequestWithoutABearerTokenHasNone() {
        MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4=");

        assertNull(resolver.resolve(new MockHttpServletRequest()));
        assertNull(resolver.resolve(basic));
    }

    private static MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/java/app/userinfo");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private static String token(Consumer<JWTClaimsSet.Builder> claims) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        claims.accept(builder);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        jwt.sign(new MACSigner("a-secret-of-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
