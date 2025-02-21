/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Class OAuth2SecurityConfiguration.
 */
@Profile(value = {"github", "cognito"})
@Configuration
public class OAuth2SecurityConfiguration {

    /** The authentication success handler. */
    private final OAuth2AuthenticationSuccessHandler authenticationSuccessHandler;

    /**
     * Instantiates a new tenants endpoint.
     *
     * @param tenantService the tenant service
     */
    @Autowired
    public OAuth2SecurityConfiguration(OAuth2AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    /**
     * Filter chain.
     *
     * @param http the http
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, HttpSecurityURIConfigurator httpSecurityURIConfigurator) throws Exception {
        http//
            .authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()))
            .oauth2Client(Customizer.withDefaults())
            .oauth2Login(oauth2 -> {
                oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userAuthoritiesMapper(userAuthoritiesMapper()));
                oauth2.successHandler(authenticationSuccessHandler);
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) new ArrayList<>(authorities).get(0);
            Map<String, Object> attributes = oidcUserAuthority.getAttributes();
            return ((ArrayList<?>) attributes.get("cognito:groups")).stream()
                                                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                                                    .collect(Collectors.toSet());
        };
    }
}
