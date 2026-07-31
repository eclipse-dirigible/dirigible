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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Re-validates {@code oauth2Login} browser sessions against the identity provider so that access
 * revoked at the IdP (user disabled, deleted or globally signed out) actually terminates the
 * session within a bounded window instead of surviving until logout.
 *
 * <p>
 * After the initial authorization-code exchange the session never consults the IdP again, so the
 * IdP's only reliable revocation signal is its refusal to honor the stored refresh token. Once the
 * session's access token expires, this filter refreshes it: a refusal invalidates the HTTP session
 * and forces re-authentication, while a successful refresh also rebuilds the principal from the
 * fresh ID token so role/group changes at the IdP are picked up without a logout. The stale window
 * is therefore bounded by the access-token lifetime, which stays configurable at the IdP.
 *
 * <p>
 * Sessions whose authorized client carries no refresh token (e.g. GitHub OAuth apps with
 * non-expiring tokens) cannot be re-validated and are passed through unchanged.
 */
public class OAuth2SessionRevalidationFilter extends OncePerRequestFilter {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2SessionRevalidationFilter.class);

    /**
     * Refresh slightly before the actual expiration, mirroring Spring's authorized client providers.
     */
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GrantedAuthoritiesMapper userAuthoritiesMapper;
    private final OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient;
    private final OidcUserService oidcUserService;
    private final JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory;
    private final SecurityContextRepository securityContextRepository;

    /**
     * Instantiates the filter for a chain without a custom user authorities mapper.
     *
     * @param authorizedClientService the store holding the tokens obtained at login
     */
    public OAuth2SessionRevalidationFilter(OAuth2AuthorizedClientService authorizedClientService) {
        this(authorizedClientService, null);
    }

    /**
     * Instantiates the filter.
     *
     * @param authorizedClientService the store holding the tokens obtained at login
     * @param userAuthoritiesMapper the mapper deriving Dirigible role authorities from the IdP user, or
     *        {@code null} to keep the authorities the user service resolves
     */
    public OAuth2SessionRevalidationFilter(OAuth2AuthorizedClientService authorizedClientService,
            GrantedAuthoritiesMapper userAuthoritiesMapper) {
        this(authorizedClientService, userAuthoritiesMapper, new RestClientRefreshTokenTokenResponseClient(), new OidcUserService(),
                new OidcIdTokenDecoderFactory(), new HttpSessionSecurityContextRepository());
    }

    OAuth2SessionRevalidationFilter(OAuth2AuthorizedClientService authorizedClientService, GrantedAuthoritiesMapper userAuthoritiesMapper,
            OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient, OidcUserService oidcUserService,
            JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory, SecurityContextRepository securityContextRepository) {
        this.authorizedClientService = authorizedClientService;
        this.userAuthoritiesMapper = userAuthoritiesMapper;
        this.refreshTokenResponseClient = refreshTokenResponseClient;
        this.oidcUserService = oidcUserService;
        this.idTokenDecoderFactory = idTokenDecoderFactory;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Re-validates the current session-based OAuth2 authentication before the request is authorized.
     * When the session is terminated the chain continues unauthenticated, so the standard entry point
     * redirects to login.
     *
     * @param request the request
     * @param response the response
     * @param chain the chain
     * @throws ServletException the servlet exception
     * @throws IOException the IO exception
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            revalidate(oauth2Authentication, request, response);
        }
        chain.doFilter(request, response);
    }

    private void revalidate(OAuth2AuthenticationToken authentication, HttpServletRequest request, HttpServletResponse response) {
        String registrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId, principalName);
        if (authorizedClient == null) {
            LOGGER.info("No authorized client for user [{}] and registration [{}] - terminating the session", principalName,
                    registrationId);
            terminateSession(request);
            return;
        }
        if (!isExpired(authorizedClient.getAccessToken())) {
            return;
        }
        if (authorizedClient.getRefreshToken() == null) {
            LOGGER.debug("No refresh token for user [{}] and registration [{}] - the session cannot be re-validated", principalName,
                    registrationId);
            return;
        }
        synchronized (WebUtils.getSessionMutex(request.getSession())) {
            // a concurrent request on the same session may have completed the re-validation already
            authorizedClient = authorizedClientService.loadAuthorizedClient(registrationId, principalName);
            if (authorizedClient == null) {
                terminateSession(request);
                return;
            }
            if (isExpired(authorizedClient.getAccessToken()) && authorizedClient.getRefreshToken() != null) {
                refresh(authentication, authorizedClient, request, response);
            }
        }
    }

    private void refresh(OAuth2AuthenticationToken authentication, OAuth2AuthorizedClient authorizedClient, HttpServletRequest request,
            HttpServletResponse response) {
        ClientRegistration clientRegistration = authorizedClient.getClientRegistration();
        String principalName = authentication.getName();
        OAuth2AccessTokenResponse tokenResponse;
        try {
            tokenResponse = refreshTokenResponseClient.getTokenResponse(new OAuth2RefreshTokenGrantRequest(clientRegistration,
                    authorizedClient.getAccessToken(), authorizedClient.getRefreshToken()));
        } catch (OAuth2AuthorizationException ex) {
            LOGGER.info("The identity provider [{}] refused to refresh the tokens of user [{}] with error [{}] - terminating the session",
                    clientRegistration.getRegistrationId(), principalName, ex.getError()
                                                                             .getErrorCode(),
                    ex);
            authorizedClientService.removeAuthorizedClient(clientRegistration.getRegistrationId(), principalName);
            terminateSession(request);
            return;
        }
        OAuth2RefreshToken refreshToken =
                tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken() : authorizedClient.getRefreshToken();
        OAuth2AuthorizedClient refreshedClient =
                new OAuth2AuthorizedClient(clientRegistration, principalName, tokenResponse.getAccessToken(), refreshToken);
        authorizedClientService.saveAuthorizedClient(refreshedClient, authentication);
        refreshAuthentication(authentication, clientRegistration, tokenResponse, request, response);
        LOGGER.debug("Re-validated the session of user [{}] against registration [{}]", principalName,
                clientRegistration.getRegistrationId());
    }

    /**
     * Rebuilds the session principal from the ID token issued with the refresh so authority changes at
     * the IdP (e.g. group membership) take effect without a logout. The IdP has just accepted the
     * refresh, so on a rebuild problem the session stays valid with the authorities from login time.
     */
    private void refreshAuthentication(OAuth2AuthenticationToken authentication, ClientRegistration clientRegistration,
            OAuth2AccessTokenResponse tokenResponse, HttpServletRequest request, HttpServletResponse response) {
        Object idTokenValue = tokenResponse.getAdditionalParameters()
                                           .get(OidcParameterNames.ID_TOKEN);
        if (idTokenValue == null || !(authentication.getPrincipal() instanceof OidcUser)) {
            return;
        }
        OAuth2AuthenticationToken refreshedAuthentication;
        try {
            Jwt jwt = idTokenDecoderFactory.createDecoder(clientRegistration)
                                           .decode(idTokenValue.toString());
            OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
            OidcUser oidcUser = oidcUserService.loadUser(new OidcUserRequest(clientRegistration, tokenResponse.getAccessToken(), idToken,
                    tokenResponse.getAdditionalParameters()));
            Collection<? extends GrantedAuthority> authorities =
                    userAuthoritiesMapper != null ? userAuthoritiesMapper.mapAuthorities(oidcUser.getAuthorities())
                            : oidcUser.getAuthorities();
            refreshedAuthentication =
                    new OAuth2AuthenticationToken(oidcUser, authorities, authentication.getAuthorizedClientRegistrationId());
            refreshedAuthentication.setDetails(authentication.getDetails());
        } catch (JwtException | OAuth2AuthenticationException ex) {
            LOGGER.warn("Failed to rebuild the principal of user [{}] from the refreshed ID token - keeping the login-time authorities",
                    authentication.getName(), ex);
            return;
        }
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(refreshedAuthentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
    }

    private static boolean isExpired(OAuth2AccessToken accessToken) {
        Instant expiresAt = accessToken.getExpiresAt();
        return expiresAt == null || Instant.now()
                                           .isAfter(expiresAt.minus(CLOCK_SKEW));
    }

    private static void terminateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
