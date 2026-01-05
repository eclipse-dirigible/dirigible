/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * The Class HttpSecurityURIConfigurator.
 */

@Component
public class HttpSecurityURIConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSecurityURIConfigurator.class);

    /** The Constant PUBLIC_PATTERNS. */
    private static final String[] PUBLIC_PATTERNS = { //
            "/", //
            "/home", //
            "/.well-known/**", //
            "/index.html", //
            "/logout", //
            "/index-busy.html", //
            "/stomp/**", //
            "/error/**", //
            "/error.html", //
            "/favicon.ico", //
            "/public/**", //
            "/webjars/**", //
            "/services/core/theme/**", //
            "/services/core/version/**", //
            "/services/core/healthcheck/**", //
            "/services/web/resources/**", //
            "/services/web/resources-locale/**", //
            "/services/web/platform-core/**", //
            "/services/web/theme-*/**", //
            "/services/js/platform-core/**", //
            "/actuator/health/liveness", //
            "/actuator/health/readiness", //
            "/actuator/health"};

    /** The Constant AUTHENTICATED_PATTERNS. */
    private static final String[] AUTHENTICATED_PATTERNS = { //
            "/services/**", //
            "/services/integrations/**", //
            "/websockets/**", //
            "/api-docs/swagger-config", //
            "/api-docs/**", //
            "/odata/**", //
            "/swagger-ui/**"};

    /** The Constant DEVELOPER_PATTERNS. */
    private static final String[] DEVELOPER_PATTERNS = { //
            "/services/bpm/**", //
            "/services/ide/**", //
            "/websockets/ide/**"};

    private static final String[] OPERATOR_PATTERNS = { //
            "/spring-admin/**", //
            "/actuator/**"};

    private final BeanProvider beanProvider;

    HttpSecurityURIConfigurator(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    public void configure(HttpSecurity http) throws Exception {
        applyCustomConfigurations(http);

        http.authorizeHttpRequests((authz) -> //

        authz.requestMatchers(PUBLIC_PATTERNS)
             .permitAll()

             // NOTE!: the order is important - role checks should be before just
             // authenticated paths

             // Fine grained configurations
             .requestMatchers(HttpMethod.GET, "/services/bpm/bpm-processes/tasks")
             .authenticated()

             .requestMatchers(HttpMethod.POST, "/services/bpm/bpm-processes/tasks/*")
             .authenticated()

             // "DEVELOPER" role required
             .requestMatchers(DEVELOPER_PATTERNS)
             .hasRole(Roles.DEVELOPER.getRoleName())

             // "OPERATOR" role required
             .requestMatchers(OPERATOR_PATTERNS)
             .hasRole(Roles.OPERATOR.getRoleName())

             // Authenticated
             .requestMatchers(AUTHENTICATED_PATTERNS)
             .authenticated()

             // Deny all other requests
             .anyRequest()
             .denyAll());
    }

    private void applyCustomConfigurations(HttpSecurity http) throws Exception {
        Collection<CustomSecurityConfigurator> customConfigurators = BeanProvider.getBeans(CustomSecurityConfigurator.class);
        for (CustomSecurityConfigurator configurator : customConfigurators) {
            LOGGER.info("Applying custom security configurations using [{}]", configurator);
            configurator.configure(http);
        }
    }

}
