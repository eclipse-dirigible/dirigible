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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.eclipse.dirigible.components.base.http.access.ProgrammaticRequestMatcher;
import org.eclipse.dirigible.components.security.oauth.ScopeRoleJwtAuthoritiesConverter;
import org.eclipse.dirigible.components.security.oauth2.IdpHintAuthorizationRequestResolver;
import org.eclipse.dirigible.components.security.oauth2.OAuth2SessionRevalidationFilter;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.BearerUnauthorizedEntryPoint;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSettings;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSupport;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantAwareAuthoritiesMapper;
import org.eclipse.dirigible.components.security.oauth2.tenant.TenantGroupsClaim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.util.StringUtils;

/**
 * The Class OAuth2SecurityConfiguration.
 */
@Profile("cognito")
@Configuration
public class CognitoSecurityConfiguration {

    /** The claim AWS Cognito puts the user groups in. */
    private static final String COGNITO_GROUPS_CLAIM = "cognito:groups";

    /** The Cognito JWKS endpoint backing the resource-server (Bearer) JWT decoder. */
    private final String jwkSetUri;

    /** The issuer of the user pool. */
    private final String issuerUri;

    /** The app client id of the platform. */
    private final String clientId;

    /** The claim the login names the user by. */
    private final String userNameAttribute;

    public CognitoSecurityConfiguration(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.client.provider.cognito.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.client.registration.cognito.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.provider.cognito.user-name-attribute}") String userNameAttribute) {
        this.jwkSetUri = jwkSetUri;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.userNameAttribute = userNameAttribute;
    }

    /**
     * Filter chain.
     *
     * @param http the http
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, HttpSecurityURIConfigurator httpSecurityURIConfigurator,
            CognitoLogoutSuccessHandler cognitoLogoutSuccessHandler, OAuth2AuthorizedClientService authorizedClientService,
            ClientRegistrationRepository clientRegistrationRepository, GrantedAuthoritiesMapper userAuthoritiesMapper,
            ResourceServerJwtSupport resourceServerJwtSupport) throws Exception {
        String loginPage = DirigibleConfig.SECURITY_LOGIN_PAGE.getStringValue();
        // both oauth2Client and oauth2Login register an authorization-request redirect filter, and
        // the client one runs first - the resolver must be set on both for the hints to pass through
        IdpHintAuthorizationRequestResolver authorizationRequestResolver =
                new IdpHintAuthorizationRequestResolver(clientRegistrationRepository);
        // registered ahead of the login and resource-server configurers, so it is the default of the
        // delegating entry point: a script that is not authenticated gets a 401 it can act on instead
        // of the redirect to the identity provider a browser navigation gets
        BearerUnauthorizedEntryPoint bearerEntryPoint = new BearerUnauthorizedEntryPoint();
        http.authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(new OAuth2SessionRevalidationFilter(authorizedClientService, userAuthoritiesMapper), AuthorizationFilter.class)
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()))
            .exceptionHandling(handling -> handling.defaultAuthenticationEntryPointFor(bearerEntryPoint, new ProgrammaticRequestMatcher()))
            .oauth2Client(oauth2Client -> oauth2Client.authorizationCodeGrant(
                    grant -> grant.authorizationRequestResolver(authorizationRequestResolver)))
            .oauth2Login(oauth2 -> {
                oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userAuthoritiesMapper(userAuthoritiesMapper));
                oauth2.authorizationEndpoint(
                        authorizationEndpoint -> authorizationEndpoint.authorizationRequestResolver(authorizationRequestResolver));
                if (StringUtils.hasText(loginPage)) {
                    oauth2.loginPage(loginPage);
                }
            })
            .oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(bearerEntryPoint)
                                                  .jwt(jwt -> jwt.decoder(resourceServerJwtSupport.decoder())
                                                                 .jwtAuthenticationConverter(
                                                                         resourceServerJwtSupport.authenticationConverter())))
            .logout(logout -> logout.deleteCookies("JSESSIONID")
                                    .invalidateHttpSession(true)
                                    .clearAuthentication(true)
                                    .logoutSuccessHandler(cognitoLogoutSuccessHandler))
            // a bearer or anonymous request must not cost a session - only the login creates one
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * The bearer token support of the Cognito profile: the decoder and the identity conversion the
     * chain and the STOMP broker share, held to {@link ResourceServerJwtSettings#cognito}.
     *
     * @param scopeRoleJwtAuthoritiesConverter the scope-to-role authorities converter
     * @param userAuthoritiesMapper the mapper deriving roles from the Cognito groups
     * @return the bearer token support
     */
    @Bean
    public ResourceServerJwtSupport resourceServerJwtSupport(ScopeRoleJwtAuthoritiesConverter scopeRoleJwtAuthoritiesConverter,
            TenantAwareAuthoritiesMapper userAuthoritiesMapper) {
        return new ResourceServerJwtSupport(ResourceServerJwtSettings.cognito(jwkSetUri, issuerUri, clientId, userNameAttribute),
                scopeRoleJwtAuthoritiesConverter, userAuthoritiesMapper);
    }

    /**
     * Maps the Cognito groups of the logged in user to authorities. What exactly is mapped depends on
     * the tenant resolution strategy - see {@link TenantAwareAuthoritiesMapper}.
     *
     * @param tenantGroupsClaim the configured groups claim
     * @return the authorities mapper
     */
    @Bean
    public TenantAwareAuthoritiesMapper userAuthoritiesMapper(TenantGroupsClaim tenantGroupsClaim) {
        return new TenantAwareAuthoritiesMapper(tenantGroupsClaim, COGNITO_GROUPS_CLAIM);
    }
}
