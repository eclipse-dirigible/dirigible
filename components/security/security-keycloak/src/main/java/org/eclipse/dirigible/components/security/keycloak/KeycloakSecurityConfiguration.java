/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

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
import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Class KeycloakSecurityConfiguration.
 */
@Profile("keycloak")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class KeycloakSecurityConfiguration {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakSecurityConfiguration.class);

    private final boolean trialModeEnabled;

    public KeycloakSecurityConfiguration() {
        trialModeEnabled = DirigibleConfig.TRIAL_ENABLED.getBooleanValue();
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
            HttpSecurityURIConfigurator httpSecurityURIConfigurator) throws Exception {
        http.authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(tenantContextInitFilter, OAuth2LoginAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.sameOrigin()))
            .oauth2Client(Customizer.withDefaults())
            .oauth2Login(Customizer.withDefaults())
            .oauth2Login(oauth2 -> {
                oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userAuthoritiesMapper(userAuthoritiesMapper()));
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
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
                List<String> keycloakGroups = (ArrayList<String>) oidcUserAuthority.getAttributes()
                                                                                   .get("groups");
                if (keycloakGroups != null) {
                    grantedAuthorities.addAll(AuthoritiesUtil.toAuthorities(keycloakGroups));
                }
            }
            return grantedAuthorities;
        };
    }
}
