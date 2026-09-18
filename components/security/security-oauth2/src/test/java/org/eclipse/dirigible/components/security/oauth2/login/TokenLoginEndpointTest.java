/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.IdentityProvider;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSettings;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSupport;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.TokenKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link TokenLoginEndpoint} - a session is minted only from a validated bearer ID
 * token, on a profile that supports bearer tokens, under the profile's default client registration.
 */
@ExtendWith(MockitoExtension.class)
class TokenLoginEndpointTest {

    private static final String REGISTRATION_ID = "cognito";

    @Mock
    private ObjectProvider<ResourceServerJwtSupport> jwtSupportProvider;

    @Mock
    private ResourceServerJwtSupport jwtSupport;

    @Mock
    private NativeLoginSessionInitializer sessionInitializer;

    private TokenLoginEndpoint endpoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        endpoint = new TokenLoginEndpoint(jwtSupportProvider, sessionInitializer);
        request = new MockHttpServletRequest("POST", "/login/token");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aValidatedIdTokenMintsTheSessionUnderTheDefaultRegistration() {
        stubSupport();
        JwtAuthenticationToken idTokenAuthentication = authenticate(jwt("id"));
        Instant expiresAt = Instant.parse("2026-09-17T12:00:00Z");
        when(sessionInitializer.establishSession(REGISTRATION_ID, idTokenAuthentication, "email", request, response)).thenReturn(expiresAt);

        ResponseEntity<Map<String, Object>> result = endpoint.login(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("AUTHENTICATED", result.getBody()
                                            .get("outcome"));
        assertEquals("2026-09-17T12:00:00Z", result.getBody()
                                                   .get("expiresAt"));
    }

    @Test
    void withoutABearerTokenNothingIsMinted() {
        when(jwtSupportProvider.getIfAvailable()).thenReturn(jwtSupport);

        ResponseEntity<Map<String, Object>> result = endpoint.login(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Bearer", result.getHeaders()
                                     .getFirst(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals("UNAUTHENTICATED", result.getBody()
                                              .get("outcome"));
        verifyNoInteractions(sessionInitializer);
    }

    @Test
    void aCookieSessionDoesNotCount() {
        when(jwtSupportProvider.getIfAvailable()).thenReturn(jwtSupport);
        setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))));

        ResponseEntity<Map<String, Object>> result = endpoint.login(request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(sessionInitializer);
    }

    @Test
    void anAccessTokenIsRefused() {
        stubSupport();
        authenticate(jwt("access"));

        ResponseEntity<Map<String, Object>> result = endpoint.login(request, response);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertEquals("ID_TOKEN_REQUIRED", result.getBody()
                                                .get("outcome"));
        verifyNoInteractions(sessionInitializer);
    }

    @Test
    void withoutBearerSupportTheEndpointAnswersNotFound() {
        when(jwtSupportProvider.getIfAvailable()).thenReturn(null);
        authenticate(jwt("id"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> endpoint.login(request, response));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(sessionInitializer);
    }

    private void stubSupport() {
        when(jwtSupportProvider.getIfAvailable()).thenReturn(jwtSupport);
        when(jwtSupport.settings()).thenReturn(new ResourceServerJwtSettings(IdentityProvider.COGNITO, "https://idp.example.org/jwks",
                "https://idp.example.org", Set.of("client-id"), Set.of(), "email", true, Set.of(TokenKind.ID, TokenKind.ACCESS),
                "cognito:groups", REGISTRATION_ID));
    }

    private static JwtAuthenticationToken authenticate(Jwt jwt) {
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")), "jane.doe@example.org");
        setAuthentication(authentication);
        return authentication;
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static Jwt jwt(String tokenUse) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("bearer-token")
                  .header("alg", "RS256")
                  .subject("3b3f18f4-1c3d-4b6e-9f9a-0e2f4c1a5d77")
                  .claim("token_use", tokenUse)
                  .claim("email", "jane.doe@example.org")
                  .issuedAt(issuedAt)
                  .expiresAt(issuedAt.plusSeconds(300))
                  .build();
    }
}
