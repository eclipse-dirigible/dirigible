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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.eclipse.dirigible.components.security.oauth2.OAuth2SessionRevalidationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import jakarta.servlet.http.HttpSession;

/**
 * Unit tests for {@link NativeLoginSessionInitializer} - the ID token validation, the session
 * minting and the authorized-client registration that puts the session under
 * {@code OAuth2SessionRevalidationFilter}'s governance.
 */
@ExtendWith(MockitoExtension.class)
class NativeLoginSessionInitializerTest {

    private static final String REGISTRATION_ID = "default-tenant";

    @Mock
    private ObjectProvider<OAuth2AuthorizedClientService> authorizedClientServiceProvider;

    @Mock
    private ObjectProvider<GrantedAuthoritiesMapper> userAuthoritiesMapperProvider;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private SecurityContextRepository securityContextRepository;

    private NativeLoginSessionInitializer initializer;
    private ClientRegistration registration;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        initializer = new NativeLoginSessionInitializer(authorizedClientServiceProvider, userAuthoritiesMapperProvider,
                idTokenDecoderFactory, securityContextRepository);
        registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                                         .clientId("client-id")
                                         .clientSecret("client-secret")
                                         .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                         .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                         .authorizationUri("https://idp.example.org/oauth2/authorize")
                                         .tokenUri("https://idp.example.org/oauth2/token")
                                         .userNameAttributeName("email")
                                         .build();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesTheSessionWithTheMappedAuthoritiesAndRegistersTheTokens() {
        when(idTokenDecoderFactory.createDecoder(registration)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode("id-token")).thenReturn(idToken());
        when(authorizedClientServiceProvider.getObject()).thenReturn(authorizedClientService);
        when(userAuthoritiesMapperProvider.getIfAvailable()).thenReturn(
                authorities -> List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")));

        initializer.establishSession(registration, new NativeLoginTokens("id-token", "access-token", "refresh-token", 3600L), request,
                response);

        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) SecurityContextHolder.getContext()
                                                                                                    .getAuthentication();
        assertEquals("jane.doe@example.org", authentication.getName());
        assertEquals(REGISTRATION_ID, authentication.getAuthorizedClientRegistrationId());
        assertTrue(authentication.getAuthorities()
                                 .contains(new SimpleGrantedAuthority("ROLE_DEVELOPER")));

        ArgumentCaptor<SecurityContext> savedContext = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(savedContext.capture(), any(), any());
        assertEquals(authentication, savedContext.getValue()
                                                 .getAuthentication());

        ArgumentCaptor<OAuth2AuthorizedClient> savedClient = ArgumentCaptor.forClass(OAuth2AuthorizedClient.class);
        verify(authorizedClientService).saveAuthorizedClient(savedClient.capture(), any());
        assertEquals("jane.doe@example.org", savedClient.getValue()
                                                        .getPrincipalName());
        assertEquals("access-token", savedClient.getValue()
                                                .getAccessToken()
                                                .getTokenValue());
        assertNotNull(savedClient.getValue()
                                 .getAccessToken()
                                 .getExpiresAt());
        assertEquals("refresh-token", savedClient.getValue()
                                                 .getRefreshToken()
                                                 .getTokenValue());
    }

    @Test
    void changesTheSessionIdOfAPreLoginSession() {
        String preLoginSessionId = request.getSession(true)
                                          .getId();
        when(idTokenDecoderFactory.createDecoder(registration)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode("id-token")).thenReturn(idToken());
        when(authorizedClientServiceProvider.getObject()).thenReturn(authorizedClientService);
        when(userAuthoritiesMapperProvider.getIfAvailable()).thenReturn(null);

        initializer.establishSession(registration, new NativeLoginTokens("id-token", "access-token", null, 3600L), request, response);

        assertNotEquals(preLoginSessionId, request.getSession(false)
                                                  .getId());
    }

    @Test
    void aBearerIdTokenMintsAFreshSessionThatEndsWithTheToken() {
        request.getSession(true)
               .setAttribute("selected-tenant", "acme");
        String carriedSessionId = request.getSession(false)
                                         .getId();
        Jwt jwt = idToken();
        JwtAuthenticationToken idTokenAuthentication =
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")), "jane.doe@example.org");

        Instant expiresAt = initializer.establishSession(registration, idTokenAuthentication, "email", request, response);

        assertEquals(jwt.getExpiresAt(), expiresAt);
        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertNotEquals(carriedSessionId, session.getId(), "the state of a carried cookie must not migrate into the token's identity");
        assertNull(session.getAttribute("selected-tenant"));
        assertEquals(expiresAt, session.getAttribute(OAuth2SessionRevalidationFilter.SESSION_EXPIRES_AT_ATTRIBUTE));

        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) SecurityContextHolder.getContext()
                                                                                                    .getAuthentication();
        assertEquals("jane.doe@example.org", authentication.getName());
        assertEquals(REGISTRATION_ID, authentication.getAuthorizedClientRegistrationId());
        assertTrue(authentication.getAuthorities()
                                 .contains(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
        assertEquals("id-token", ((OidcUser) authentication.getPrincipal()).getIdToken()
                                                                           .getTokenValue());
        verify(securityContextRepository).saveContext(any(SecurityContext.class), any(), any());
        verifyNoInteractions(authorizedClientServiceProvider, authorizedClientService);
    }

    @Test
    void aBearerIdTokenMintsASessionWhereNoneWasCarried() {
        JwtAuthenticationToken idTokenAuthentication = new JwtAuthenticationToken(idToken(), List.of(), "jane.doe@example.org");

        initializer.establishSession(registration, idTokenAuthentication, "email", request, response);

        assertNotNull(request.getSession(false));
        assertNotNull(request.getSession(false)
                             .getAttribute(OAuth2SessionRevalidationFilter.SESSION_EXPIRES_AT_ATTRIBUTE));
    }

    @Test
    void anInvalidIdTokenEstablishesNothing() {
        when(idTokenDecoderFactory.createDecoder(registration)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode("id-token")).thenThrow(new JwtException("expired"));

        NativeLoginException exception = assertThrows(NativeLoginException.class, () -> initializer.establishSession(registration,
                new NativeLoginTokens("id-token", "access-token", "refresh-token", 3600L), request, response));

        assertEquals(NativeLoginException.Outcome.AUTHENTICATION_FAILED, exception.getOutcome());
        assertNull(SecurityContextHolder.getContext()
                                        .getAuthentication());
        verifyNoInteractions(securityContextRepository, authorizedClientService);
    }

    private static Jwt idToken() {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("id-token")
                  .header("alg", "RS256")
                  .subject("3b3f18f4-1c3d-4b6e-9f9a-0e2f4c1a5d77")
                  .claim("email", "jane.doe@example.org")
                  .claim("cognito:groups", List.of("DEVELOPER"))
                  .issuedAt(issuedAt)
                  .expiresAt(issuedAt.plusSeconds(300))
                  .build();
    }
}
