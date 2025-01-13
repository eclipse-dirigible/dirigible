/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.security;

import org.eclipse.dirigible.components.base.http.access.CorsConfigurationSourceProvider;
import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The Class WebSecurityConfig.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "basic.enabled", havingValue = "true")
public class BasicSecurityConfig {

    /**
     * Filter chain.
     *
     * @param http the http
     * @param tenantContextInitFilter the tenant context init filter
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TenantContextInitFilter tenantContextInitFilter) throws Exception {
        http.cors(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())// if enabled, some functionalities will not work - like creating a project
            .addFilterBefore(tenantContextInitFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(Customizer.withDefaults())
            .logout(logout -> logout.deleteCookies("JSESSIONID"))
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()));

        HttpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * Cors configuration source.
     *
     * @return the cors configuration source
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        return CorsConfigurationSourceProvider.get();
    }
}
