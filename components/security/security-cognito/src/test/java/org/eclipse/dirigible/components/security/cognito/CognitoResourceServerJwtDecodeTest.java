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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.security.oauth.ScopeRoleJwtAuthoritiesConverter;
import org.eclipse.dirigible.components.security.service.ScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * Regression test for the resource-server (Bearer) JWT decode path.
 *
 * <p>
 * Reproduces the exact mechanism {@code CognitoSecurityConfiguration} now uses: a JWKS-backed
 * {@link NimbusJwtDecoder} built from a {@code jwk-set-uri} decodes a Cognito-style RS256 access
 * token, after which the {@link ScopeRoleJwtAuthoritiesConverter} maps its {@code scope} claim to
 * Dirigible role authorities. This guards against the regression where the resource server resolved
 * a foreign {@code JwtDecoder} (the embedded authorization server's self-signed one) and rejected
 * every valid token at decode with "no matching key(s) found".
 */
@ExtendWith(MockitoExtension.class)
class CognitoResourceServerJwtDecodeTest {

    private static final String KEY_ID = "test-signing-key";

    @Mock
    private ScopeService scopeService;

    @Test
    void resourceServerDecodesRs256JwtFromJwkSetAndAppliesScopeRoles() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID(KEY_ID)
                                                 .algorithm(JWSAlgorithm.RS256)
                                                 .generate();
        HttpServer jwksServer = startJwksServer(new JWKSet(rsaKey.toPublicJWK()).toString());
        try {
            String jwkSetUri = "http://localhost:" + jwksServer.getAddress()
                                                               .getPort()
                    + "/.well-known/jwks.json";
            String token = signRs256(rsaKey,
                    "sample-resource-server/ADMINISTRATOR sample-resource-server/sample-app.Orders.OrderFullAccess openid");

            // The decoder built exactly as the resource-server config builds it.
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                                                       .build();
            Jwt decoded = decoder.decode(token);
            assertEquals("m2m-client", decoded.getSubject());

            // The scope -> role authorities applied on top of the decoded token.
            when(scopeService.getScopeRolesMappings()).thenReturn(Collections.emptyMap());
            Collection<GrantedAuthority> authorities = new ScopeRoleJwtAuthoritiesConverter(scopeService).convert(decoded);

            Set<String> names = authorities.stream()
                                           .map(GrantedAuthority::getAuthority)
                                           .collect(Collectors.toSet());
            assertEquals(Set.of("ROLE_ADMINISTRATOR", "ROLE_sample-app.Orders.OrderFullAccess"), names);
            assertTrue(names.stream()
                            .noneMatch(n -> n.contains("openid")));
        } finally {
            jwksServer.stop(0);
        }
    }

    private HttpServer startJwksServer(String jwksJson) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        byte[] body = jwksJson.getBytes(StandardCharsets.UTF_8);
        server.createContext("/.well-known/jwks.json", exchange -> {
            exchange.getResponseHeaders()
                    .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }

    private String signRs256(RSAKey rsaKey, String scope) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("m2m-client")
                                                        .claim("token_use", "access")
                                                        .claim("scope", scope)
                                                        .issueTime(Date.from(now))
                                                        .expirationTime(Date.from(now.plusSeconds(3600)))
                                                        .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID)
                                                                                     .build(),
                claims);
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }
}
