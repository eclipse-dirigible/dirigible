/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Unit tests for {@link OAuth2SessionRevalidationFilter} - the re-validation of oauth2Login
 * sessions against the identity provider via the stored refresh token.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2SessionRevalidationFilterTest {

    private static final String REGISTRATION_ID = "test";
    private static final String PRINCIPAL_NAME = "test-user";

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private GrantedAuthoritiesMapper userAuthoritiesMapper;

    @Mock
    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient;

    @Mock
    private OidcUserService oidcUserService;

    @Mock
    private JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private JwtDecoder jwtDecoder;

    private OAuth2SessionRevalidationFilter filter;
    private ClientRegistration clientRegistration;
    private OAuth2AuthenticationToken authentication;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new OAuth2SessionRevalidationFilter(authorizedClientService, userAuthoritiesMapper, refreshTokenResponseClient,
                oidcUserService, idTokenDecoderFactory, securityContextRepository);
        clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                                               .clientId("test-client")
                                               .clientSecret("test-secret")
                                               .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                               .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                               .authorizationUri("https://idp.example.org/oauth2/authorize")
                                               .tokenUri("https://idp.example.org/oauth2/token")
                                               .userNameAttributeName("sub")
                                               .build();
        authentication = new OAuth2AuthenticationToken(oidcUser("old-id-token"), List.of(new SimpleGrantedAuthority("ROLE_LOGIN_TIME")),
                REGISTRATION_ID);
        setAuthentication(authentication);
        session = new MockHttpSession();
        request = new MockHttpServletRequest();
        request.setSession(session);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughNonOAuth2Authentication() throws Exception {
        Authentication basicAuthentication = new UsernamePasswordAuthenticationToken("admin", "admin", List.of());
        setAuthentication(basicAuthentication);

        filter.doFilter(request, response, chain);

        verifyNoInteractions(authorizedClientService, refreshTokenResponseClient);
        assertChainContinuedWith(basicAuthentication);
        assertFalse(session.isInvalid());
    }

    @Test
    void passesThroughWhenAccessTokenIsFresh() throws Exception {
        stubAuthorizedClient(freshAccessToken(), refreshToken());

        filter.doFilter(request, response, chain);

        verifyNoInteractions(refreshTokenResponseClient);
        assertChainContinuedWith(authentication);
        assertFalse(session.isInvalid());
    }

    @Test
    void passesThroughWhenExpiredButNoRefreshTokenAvailable() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), null);

        filter.doFilter(request, response, chain);

        verifyNoInteractions(refreshTokenResponseClient);
        assertChainContinuedWith(authentication);
        assertFalse(session.isInvalid());
    }

    @Test
    void terminatesSessionWhenAuthorizedClientIsMissing() throws Exception {
        when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext()
                                        .getAuthentication());
        assertChainContinued();
    }

    @Test
    void terminatesSessionWhenIdPRefusesRefresh() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), refreshToken());
        when(refreshTokenResponseClient.getTokenResponse(any(OAuth2RefreshTokenGrantRequest.class))).thenThrow(
                new OAuth2AuthorizationException(new OAuth2Error("invalid_grant")));

        filter.doFilter(request, response, chain);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext()
                                        .getAuthentication());
        verify(authorizedClientService).removeAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);
        verify(authorizedClientService, never()).saveAuthorizedClient(any(), any());
        assertChainContinued();
    }

    @Test
    void savesRefreshedTokensAndKeepsSessionOnSuccessfulRefresh() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), refreshToken());
        when(refreshTokenResponseClient.getTokenResponse(any(OAuth2RefreshTokenGrantRequest.class))).thenReturn(
                tokenResponse("new-refresh-token", null));

        filter.doFilter(request, response, chain);

        OAuth2AuthorizedClient savedClient = captureSavedClient();
        assertEquals("new-access-token", savedClient.getAccessToken()
                                                    .getTokenValue());
        assertEquals("new-refresh-token", savedClient.getRefreshToken()
                                                     .getTokenValue());
        assertFalse(session.isInvalid());
        assertChainContinuedWith(authentication);
    }

    @Test
    void retainsPreviousRefreshTokenWhenResponseOmitsIt() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), refreshToken());
        when(refreshTokenResponseClient.getTokenResponse(any(OAuth2RefreshTokenGrantRequest.class))).thenReturn(tokenResponse(null, null));

        filter.doFilter(request, response, chain);

        OAuth2AuthorizedClient savedClient = captureSavedClient();
        assertEquals("old-refresh-token", savedClient.getRefreshToken()
                                                     .getTokenValue());
        assertFalse(session.isInvalid());
        assertChainContinuedWith(authentication);
    }

    @Test
    void rebuildsPrincipalAndAuthoritiesFromRefreshedIdToken() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), refreshToken());
        when(refreshTokenResponseClient.getTokenResponse(any(OAuth2RefreshTokenGrantRequest.class))).thenReturn(
                tokenResponse("new-refresh-token", "new-id-token"));
        when(idTokenDecoderFactory.createDecoder(clientRegistration)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode("new-id-token")).thenReturn(jwt("new-id-token"));
        OidcUser refreshedUser = oidcUser("new-id-token");
        when(oidcUserService.loadUser(any(OidcUserRequest.class))).thenReturn(refreshedUser);
        List<GrantedAuthority> refreshedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_REFRESHED"));
        doReturn(refreshedAuthorities).when(userAuthoritiesMapper)
                                      .mapAuthorities(any());

        filter.doFilter(request, response, chain);

        Authentication currentAuthentication = SecurityContextHolder.getContext()
                                                                    .getAuthentication();
        assertNotSame(authentication, currentAuthentication);
        assertSame(refreshedUser, currentAuthentication.getPrincipal());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_REFRESHED")), List.copyOf(currentAuthentication.getAuthorities()));
        verify(securityContextRepository).saveContext(any(SecurityContext.class), any(), any());
        assertFalse(session.isInvalid());
        assertChainContinued();
    }

    @Test
    void keepsLoginTimeAuthoritiesWhenRefreshedIdTokenCannotBeProcessed() throws Exception {
        stubAuthorizedClient(expiredAccessToken(), refreshToken());
        when(refreshTokenResponseClient.getTokenResponse(any(OAuth2RefreshTokenGrantRequest.class))).thenReturn(
                tokenResponse("new-refresh-token", "new-id-token"));
        when(idTokenDecoderFactory.createDecoder(clientRegistration)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode("new-id-token")).thenThrow(new JwtException("invalid token"));

        filter.doFilter(request, response, chain);

        assertFalse(session.isInvalid());
        assertChainContinuedWith(authentication);
        assertEquals("new-access-token", captureSavedClient().getAccessToken()
                                                             .getTokenValue());
    }

    private void stubAuthorizedClient(OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken) {
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, PRINCIPAL_NAME, accessToken, refreshToken);
        when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(authorizedClient);
    }

    private OAuth2AuthorizedClient captureSavedClient() {
        ArgumentCaptor<OAuth2AuthorizedClient> captor = ArgumentCaptor.forClass(OAuth2AuthorizedClient.class);
        verify(authorizedClientService).saveAuthorizedClient(captor.capture(), any(Authentication.class));
        return captor.getValue();
    }

    private void assertChainContinued() {
        assertSame(request, chain.getRequest());
    }

    private void assertChainContinuedWith(Authentication expectedAuthentication) {
        assertChainContinued();
        assertSame(expectedAuthentication, SecurityContextHolder.getContext()
                                                                .getAuthentication());
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static OidcUser oidcUser(String idTokenValue) {
        OidcIdToken idToken = new OidcIdToken(idTokenValue, Instant.now(), Instant.now()
                                                                                  .plusSeconds(300),
                Map.of("sub", PRINCIPAL_NAME));
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
    }

    private static OAuth2AccessToken expiredAccessToken() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "expired-access-token", Instant.now()
                                                                                                        .minusSeconds(3600),
                Instant.now()
                       .minusSeconds(60));
    }

    private static OAuth2AccessToken freshAccessToken() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "fresh-access-token", Instant.now(), Instant.now()
                                                                                                                     .plusSeconds(3600));
    }

    private static OAuth2RefreshToken refreshToken() {
        return new OAuth2RefreshToken("old-refresh-token", Instant.now());
    }

    private static OAuth2AccessTokenResponse tokenResponse(String refreshTokenValue, String idTokenValue) {
        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken("new-access-token")
                                                                             .tokenType(OAuth2AccessToken.TokenType.BEARER)
                                                                             .expiresIn(300);
        if (refreshTokenValue != null) {
            builder.refreshToken(refreshTokenValue);
        }
        if (idTokenValue != null) {
            builder.additionalParameters(Map.of(OidcParameterNames.ID_TOKEN, idTokenValue));
        }
        return builder.build();
    }

    private static Jwt jwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                  .header("alg", "RS256")
                  .subject(PRINCIPAL_NAME)
                  .issuedAt(Instant.now())
                  .expiresAt(Instant.now()
                                    .plusSeconds(300))
                  .claim("cognito:groups", List.of("Developer"))
                  .build();
    }
}
