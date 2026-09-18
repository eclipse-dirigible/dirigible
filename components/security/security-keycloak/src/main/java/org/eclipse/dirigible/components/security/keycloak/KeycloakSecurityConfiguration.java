/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

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
import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.util.StringUtils;

/**
 * The Class KeycloakSecurityConfiguration.
 */
@Profile("keycloak")
@Configuration
@EnableWebSecurity
public class KeycloakSecurityConfiguration {

    /** The claim a Keycloak realm typically puts the user groups in. */
    private static final String KEYCLOAK_GROUPS_CLAIM = "groups";

    /** The Keycloak JWKS endpoint backing the resource-server (Bearer) JWT decoder. */
    private final String jwkSetUri;

    /** The issuer of the realm. */
    private final String issuerUri;

    /** The client id of the platform. */
    private final String clientId;

    /** The claim the login names the user by. */
    private final String userNameAttribute;

    public KeycloakSecurityConfiguration(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.client.registration.keycloak.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.provider.keycloak.user-name-attribute}") String userNameAttribute) {
        this.jwkSetUri = jwkSetUri;
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.userNameAttribute = userNameAttribute;
    }

    /**
     * Configure.
     *
     * @param http the http
     * @param tenantContextInitFilter the tenant context init filter
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    SecurityFilterChain configure(HttpSecurity http, TenantContextInitFilter tenantContextInitFilter,
            HttpSecurityURIConfigurator httpSecurityURIConfigurator, KeycloakLogoutSuccessHandler keycloakLogoutSuccessHandler,
            OAuth2AuthorizedClientService authorizedClientService, ClientRegistrationRepository clientRegistrationRepository,
            GrantedAuthoritiesMapper userAuthoritiesMapper, ResourceServerJwtSupport resourceServerJwtSupport) throws Exception {
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
            .addFilterBefore(tenantContextInitFilter, OAuth2LoginAuthenticationFilter.class)
            .addFilterBefore(new OAuth2SessionRevalidationFilter(authorizedClientService, userAuthoritiesMapper), AuthorizationFilter.class)
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.sameOrigin()))
            .exceptionHandling(handling -> handling.defaultAuthenticationEntryPointFor(bearerEntryPoint, new ProgrammaticRequestMatcher()))
            .oauth2Client(oauth2Client -> oauth2Client.authorizationCodeGrant(
                    grant -> grant.authorizationRequestResolver(authorizationRequestResolver)))
            .oauth2Login(Customizer.withDefaults())
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
                                    .logoutSuccessHandler(keycloakLogoutSuccessHandler))
            // a bearer or anonymous request must not cost a session - only the login creates one
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * The bearer token support of the Keycloak profile: the decoder and the identity conversion the
     * chain and the STOMP broker share, held to {@link ResourceServerJwtSettings#keycloak}.
     *
     * @param scopeRoleJwtAuthoritiesConverter the scope-to-role authorities converter
     * @param userAuthoritiesMapper the mapper deriving roles from the Keycloak groups
     * @return the bearer token support
     */
    @Bean
    public ResourceServerJwtSupport resourceServerJwtSupport(ScopeRoleJwtAuthoritiesConverter scopeRoleJwtAuthoritiesConverter,
            TenantAwareAuthoritiesMapper userAuthoritiesMapper) {
        return new ResourceServerJwtSupport(ResourceServerJwtSettings.keycloak(jwkSetUri, issuerUri, clientId, userNameAttribute),
                scopeRoleJwtAuthoritiesConverter, userAuthoritiesMapper);
    }

    /**
     * Maps the Keycloak groups of the logged in user to authorities. What exactly is mapped depends on
     * the tenant resolution strategy - see {@link TenantAwareAuthoritiesMapper}.
     *
     * @param tenantGroupsClaim the configured groups claim
     * @return the authorities mapper
     */
    @Bean
    public TenantAwareAuthoritiesMapper userAuthoritiesMapper(TenantGroupsClaim tenantGroupsClaim) {
        return new TenantAwareAuthoritiesMapper(tenantGroupsClaim, KEYCLOAK_GROUPS_CLAIM);
    }
}
