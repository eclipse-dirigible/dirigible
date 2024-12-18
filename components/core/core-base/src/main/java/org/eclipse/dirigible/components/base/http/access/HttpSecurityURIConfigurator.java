/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * The Class HttpSecurityURIConfigurator.
 */
public class HttpSecurityURIConfigurator {

    /** The Constant PUBLIC_PATTERNS. */
    private static final String[] PUBLIC_PATTERNS = {//
            "/", //
            "/home", //
            "/index.html", //
            "/logout", //
            "/index-busy.html", //
            "/stomp", //
            "/error/**", //
            "/error.html", //
            "/favicon.ico", //
            "/public/**", //
            "/webjars/**", //
            "/services/core/theme/**", //
            "/services/core/version/**", //
            "/services/core/healthcheck/**", //
            "/services/web/resources/**", //
            "/services/web/resources-core/**", //
            "/services/web/platform-core/**", //
            "/services/js/platform-core/**", //
            "/services/js/resources-core/**", //
            "/services/integrations/**", //
            "/actuator/health/liveness", //
            "/actuator/health/readiness", //
            "/actuator/health"};

    /** The Constant AUTHENTICATED_PATTERNS. */
    private static final String[] AUTHENTICATED_PATTERNS = {//
            "/services/**", //
            "/websockets/**", //
            "/api-docs/swagger-config", //
            "/api-docs/**", //
            "/odata/**", //
            "/swagger-ui/**"};

    /** The Constant DEVELOPER_PATTERNS. */
    private static final String[] DEVELOPER_PATTERNS = {//
            "/services/bpm/**", //
            "/services/ide/**", //
            "/websockets/ide/**"};

    private static final String[] OPERATOR_PATTERNS = {//
            "/spring-admin/**", //
            "/actuator/**"};

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    public static void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authz) -> //
        authz.requestMatchers(PUBLIC_PATTERNS)
             .permitAll()

             // NOTE!: the order is important - role checks should be before just authenticated paths

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

}
