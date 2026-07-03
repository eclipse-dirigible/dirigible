/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.security;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.dirigible.components.base.http.access.CorsConfigurationSourceProvider;
import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.eclipse.dirigible.components.tenants.tenant.TenantContextInitFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The Class WebSecurityConfig.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "basic.enabled", havingValue = "true")
@EnableMethodSecurity(securedEnabled = false, jsr250Enabled = true, prePostEnabled = false)
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
    SecurityFilterChain filterChain(HttpSecurity http, TenantContextInitFilter tenantContextInitFilter,
            HttpSecurityURIConfigurator httpSecurityURIConfigurator) throws Exception {
        http.cors(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())// if enabled, some functionalities will not work - like creating a project
            .addFilterBefore(tenantContextInitFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(Customizer.withDefaults())
            .logout(logout -> logout.deleteCookies("JSESSIONID"))
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()))
            // A programmatic (fetch/XHR) request whose session expired must get a PLAIN 401 - the
            // default Basic entry point's `WWW-Authenticate: Basic` challenge makes the BROWSER pop
            // its native login dialog before any script sees the response (the generated apps poll
            // the inbox every 30s, so an idle tab surfaced the dialog "out of nowhere"). Browser
            // navigations don't match and keep the normal Basic/form login flow.
            .exceptionHandling(handling -> handling.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    BasicSecurityConfig::isProgrammaticRequest));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * Whether the request comes from script (fetch / XMLHttpRequest) rather than a browser navigation:
     * every modern browser stamps programmatic requests with a {@code Sec-Fetch-Mode} other than
     * {@code navigate}; the {@code X-Requested-With} header is the legacy client-sent marker kept as a
     * fallback.
     *
     * @param request the inbound request
     * @return true for a programmatic request
     */
    private static boolean isProgrammaticRequest(HttpServletRequest request) {
        String secFetchMode = request.getHeader("Sec-Fetch-Mode");
        if (secFetchMode != null) {
            return !"navigate".equalsIgnoreCase(secFetchMode);
        }
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
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
