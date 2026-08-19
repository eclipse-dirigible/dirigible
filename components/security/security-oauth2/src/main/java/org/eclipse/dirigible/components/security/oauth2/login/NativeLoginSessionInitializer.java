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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.eclipse.dirigible.components.security.oauth2.OAuth2SessionRevalidationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Establishes the standard platform session from the tokens a {@link NativeLoginProvider} obtained
 * - the same authentication the {@code oauth2Login} authorization-code callback establishes.
 *
 * <p>
 * The ID token is validated against the registration's JWKS (issuer, audience, expiry) before
 * anything else happens. The principal is built from the registration's user-name attribute and the
 * authorities from the profile's {@link GrantedAuthoritiesMapper}, the session id is changed
 * against session fixation, and the tokens are registered as an {@link OAuth2AuthorizedClient} so
 * {@link OAuth2SessionRevalidationFilter} governs the session lifetime exactly as for a hosted
 * login. Tokens never reach the browser - the client receives only the session cookie.
 */
@Component
class NativeLoginSessionInitializer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoginSessionInitializer.class);

    private final ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService;
    private final ObjectProvider<GrantedAuthoritiesMapper> userAuthoritiesMapper;
    private final JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory;
    private final SecurityContextRepository securityContextRepository;

    /**
     * Instantiates the session initializer.
     *
     * @param authorizedClientService the store the tokens are registered into, resolved lazily because
     *        it only exists on the OAuth2 profiles
     * @param userAuthoritiesMapper the profile's authorities mapper, when one is defined
     */
    @Autowired
    NativeLoginSessionInitializer(ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService,
            ObjectProvider<GrantedAuthoritiesMapper> userAuthoritiesMapper) {
        this(authorizedClientService, userAuthoritiesMapper, new OidcIdTokenDecoderFactory(), new HttpSessionSecurityContextRepository());
    }

    NativeLoginSessionInitializer(ObjectProvider<OAuth2AuthorizedClientService> authorizedClientService,
            ObjectProvider<GrantedAuthoritiesMapper> userAuthoritiesMapper, JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory,
            SecurityContextRepository securityContextRepository) {
        this.authorizedClientService = authorizedClientService;
        this.userAuthoritiesMapper = userAuthoritiesMapper;
        this.idTokenDecoderFactory = idTokenDecoderFactory;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Establishes the authenticated session for the user the tokens were issued to.
     *
     * @param registration the client registration the login ran against
     * @param tokens the provider-issued tokens
     * @param request the request
     * @param response the response
     * @throws NativeLoginException when the ID token fails validation
     */
    void establishSession(ClientRegistration registration, NativeLoginTokens tokens, HttpServletRequest request,
            HttpServletResponse response) {
        OidcIdToken idToken = validateIdToken(registration, tokens.idToken());
        OAuth2AuthenticationToken authentication = buildAuthentication(registration, idToken, request);

        if (request.getSession(false) != null) {
            // session-fixation protection: never keep the id a pre-login session was known under
            request.changeSessionId();
        }
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        registerAuthorizedClient(registration, authentication, tokens);
        LOGGER.debug("Established a native login session for user [{}] on registration [{}]", authentication.getName(),
                registration.getRegistrationId());
    }

    private OidcIdToken validateIdToken(ClientRegistration registration, String idTokenValue) {
        try {
            Jwt jwt = idTokenDecoderFactory.createDecoder(registration)
                                           .decode(idTokenValue);
            return new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
        } catch (JwtException ex) {
            LOGGER.error("The ID token issued for a native login on registration [{}] failed validation", registration.getRegistrationId(),
                    ex);
            throw new NativeLoginException(NativeLoginException.Outcome.AUTHENTICATION_FAILED, "ID token validation failed", ex);
        }
    }

    private OAuth2AuthenticationToken buildAuthentication(ClientRegistration registration, OidcIdToken idToken,
            HttpServletRequest request) {
        OidcUserAuthority oidcUserAuthority = new OidcUserAuthority(idToken);
        GrantedAuthoritiesMapper authoritiesMapper = userAuthoritiesMapper.getIfAvailable();
        Collection<? extends GrantedAuthority> authorities =
                authoritiesMapper != null ? authoritiesMapper.mapAuthorities(List.of(oidcUserAuthority)) : List.of(oidcUserAuthority);
        String userNameAttributeName = registration.getProviderDetails()
                                                   .getUserInfoEndpoint()
                                                   .getUserNameAttributeName();
        OidcUser oidcUser = StringUtils.hasText(userNameAttributeName) ? new DefaultOidcUser(authorities, idToken, userNameAttributeName)
                : new DefaultOidcUser(authorities, idToken);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(oidcUser, authorities, registration.getRegistrationId());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private void registerAuthorizedClient(ClientRegistration registration, OAuth2AuthenticationToken authentication,
            NativeLoginTokens tokens) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = tokens.expiresInSeconds() != null ? issuedAt.plusSeconds(tokens.expiresInSeconds()) : null;
        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, tokens.accessToken(), issuedAt, expiresAt);
        OAuth2RefreshToken refreshToken = tokens.refreshToken() != null ? new OAuth2RefreshToken(tokens.refreshToken(), issuedAt) : null;
        authorizedClientService.getObject()
                               .saveAuthorizedClient(
                                       new OAuth2AuthorizedClient(registration, authentication.getName(), accessToken, refreshToken),
                                       authentication);
    }
}
