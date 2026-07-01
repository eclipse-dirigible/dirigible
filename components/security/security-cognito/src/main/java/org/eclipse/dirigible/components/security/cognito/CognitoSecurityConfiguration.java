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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.security.oauth.ScopeRoleJwtAuthoritiesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Class OAuth2SecurityConfiguration.
 */
@Profile("cognito")
@Configuration
public class CognitoSecurityConfiguration {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CognitoSecurityConfiguration.class);

    private final boolean trialModeEnabled;

    /** The Cognito JWKS endpoint backing the resource-server (Bearer) JWT decoder. */
    private final String jwkSetUri;

    public CognitoSecurityConfiguration(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        this.trialModeEnabled = DirigibleConfig.TRIAL_ENABLED.getBooleanValue();
        this.jwkSetUri = jwkSetUri;
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
            ScopeRoleJwtAuthoritiesConverter scopeRoleJwtAuthoritiesConverter, CognitoLogoutSuccessHandler cognitoLogoutSuccessHandler)
            throws Exception {
        http.authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()))
            .oauth2Client(Customizer.withDefaults())
            .oauth2Login(oauth2 -> {
                oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userAuthoritiesMapper(userAuthoritiesMapper()));
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())
                                                                 .jwtAuthenticationConverter(
                                                                         jwtAuthenticationConverter(scopeRoleJwtAuthoritiesConverter))))
            .logout(logout -> logout.deleteCookies("JSESSIONID")
                                    .invalidateHttpSession(true)
                                    .clearAuthentication(true)
                                    .logoutSuccessHandler(cognitoLogoutSuccessHandler))
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * Builds the JWT authentication converter that derives Dirigible role authorities from the
     * validated {@code scope} claim (machine-to-machine / client-credentials tokens). Token validation
     * is unaffected.
     *
     * @param scopeRoleJwtAuthoritiesConverter the scope-to-role authorities converter
     * @return the JWT authentication converter
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter(ScopeRoleJwtAuthoritiesConverter scopeRoleJwtAuthoritiesConverter) {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(scopeRoleJwtAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * Builds the resource-server JWT decoder explicitly from the Cognito JWKS endpoint (default JWS
     * algorithm RS256).
     *
     * <p>
     * This must be pinned on the configurer rather than relying on a {@link JwtDecoder} bean: the
     * embedded Spring Authorization Server publishes its own {@code JwtDecoder} (backed by its
     * in-memory keys), and the Spring Boot OAuth2 resource-server auto-configuration is not on the
     * classpath, so {@code getBean(JwtDecoder.class)} would otherwise resolve the authorization
     * server's decoder and reject every Cognito token. Validation (signature/expiry against
     * {@code jwk-set-uri}) is unchanged.
     *
     * @return the Cognito JWKS-backed JWT decoder
     */
    private JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                               .build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
            if (trialModeEnabled) {
                LOGGER.debug("Trial enabled - returning all available system roles for the current user.");
                grantedAuthorities.addAll(AuthoritiesUtil.toAuthorities(Arrays.stream(Roles.values())
                                                                              .map(Roles::getRoleName)
                                                                              .collect(Collectors.toSet())));
            } else {
                OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) new ArrayList<>(authorities).get(0);
                List<String> cognitoGroups = (ArrayList<String>) oidcUserAuthority.getAttributes()
                                                                                  .get("cognito:groups");
                if (cognitoGroups != null) {
                    grantedAuthorities.addAll(AuthoritiesUtil.toAuthorities(cognitoGroups));
                }
            }
            return grantedAuthorities;
        };
    }
}
