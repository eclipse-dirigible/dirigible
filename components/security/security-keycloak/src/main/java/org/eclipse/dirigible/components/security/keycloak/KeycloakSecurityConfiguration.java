/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Class KeycloakSecurityConfiguration.
 */
@Profile("keycloak")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class KeycloakSecurityConfiguration {

    /**
     * Configure.
     *
     * @param http the http
     * @param tenantContextInitFilter the tenant context init filter
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    SecurityFilterChain configure(HttpSecurity http, TenantContextInitFilter tenantContextInitFilter) throws Exception {
        http//
            .authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(tenantContextInitFilter, OAuth2LoginAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.sameOrigin()))
            .oauth2Client(Customizer.withDefaults())
            .oauth2Login(Customizer.withDefaults())
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        HttpSecurityURIConfigurator.configure(http);

        return http.build();
    }
}
