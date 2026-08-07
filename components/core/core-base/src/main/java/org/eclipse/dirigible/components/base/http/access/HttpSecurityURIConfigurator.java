/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.http.uri.HostedEngineUris;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            "/services/core/readiness/**", //
            "/services/web/resources/**", //
            "/services/web/resources-locale/**", //
            "/services/web/platform-core/**", //
            "/services/web/application-core/**", //
            "/services/web/editor-monaco/**", //
            "/services/web/service-workspace/**", //
            "/services/web/service-repository/**", //
            "/services/web/theme-*/**", //
            "/services/js/platform-core/**", //
            "/services/js/application-core/**", //
            "/services/js/platform-branding/**", //
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
            "/swagger-ui/**"};

    /**
     * Monitoring surface. The controllers behind these paths all declare
     * {@code @RolesAllowed({ADMINISTRATOR, DEVELOPER, OPERATOR})}, but they live under
     * {@code /services/ide/**} and {@code /services/bpm/**}, which the URL layer gates on DEVELOPER
     * alone - so an OPERATOR was rejected before method security ever ran. These patterns re-align the
     * URL layer with the declared method policy; they are matched before {@link #DEVELOPER_PATTERNS},
     * so the rest of those prefixes (workspaces, git, publisher, the BPMN modeler, ...) stays
     * DEVELOPER-only.
     */
    private static final String[] MONITORING_PATTERNS = { //
            "/services/bpm/bpm-processes", //
            "/services/bpm/bpm-processes/**", //
            "/services/ide/monitoring", //
            "/services/ide/monitoring/**", //
            "/services/ide/logs", //
            "/services/ide/logs/**", //
            "/services/ide/loggers", //
            "/services/ide/loggers/**", //
            "/services/ide/messaging-monitoring", //
            "/services/ide/messaging-monitoring/**", //
            "/websockets/ide/console"};

    /** The Constant DEVELOPER_PATTERNS. */
    private static final String[] DEVELOPER_PATTERNS = { //
            "/services/bpm/**", //
            "/services/ide/**", //
            "/websockets/ide/**"};

    private static final String[] OPERATOR_PATTERNS = { //
            "/spring-admin/**", //
            "/actuator/**"};

    /**
     * Native-app management surface. Any of DEVELOPER, ADMINISTRATOR or OPERATOR may inspect / start /
     * stop / delete native applications via {@code /services/native-apps/**}.
     */
    private static final String[] NATIVE_APPS_MANAGEMENT_PATTERNS = { //
            "/services/native-apps", //
            "/services/native-apps/**"};

    /** The roles allowed on the monitoring and native-app management surfaces. */
    private static final String[] OPERATIONS_ROLES = { //
            Roles.ADMINISTRATOR.getRoleName(), //
            Roles.DEVELOPER.getRoleName(), //
            Roles.OPERATOR.getRoleName()};

    /**
     * The role gates in the order they are applied - the first gate whose pattern matches a request
     * decides the roles required for it. Both {@link #configure(HttpSecurity)} and the test that guards
     * the matrix read this single declaration.
     */
    static final List<RoleGate> ROLE_GATES = List.of( //
            new RoleGate(MONITORING_PATTERNS, OPERATIONS_ROLES), //
            new RoleGate(DEVELOPER_PATTERNS, new String[] {Roles.DEVELOPER.getRoleName()}), //
            new RoleGate(OPERATOR_PATTERNS, new String[] {Roles.OPERATOR.getRoleName()}), //
            new RoleGate(NATIVE_APPS_MANAGEMENT_PATTERNS, OPERATIONS_ROLES));

    /**
     * A role gate - the URI patterns it covers and the roles any of which grants access to them.
     *
     * @param patterns the Ant patterns
     * @param roles the role names, any of which is sufficient
     */
    record RoleGate(String[] patterns, String[] roles) {
    }

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

        http.authorizeHttpRequests((authz) -> {

            authz.requestMatchers(PUBLIC_PATTERNS)
                 .permitAll();

            // NOTE!: the order is important - role checks should be before just
            // authenticated paths

            // Fine grained configurations
            authz.requestMatchers(HttpMethod.GET, "/services/bpm/bpm-processes/tasks")
                 .authenticated();

            authz.requestMatchers(HttpMethod.POST, "/services/bpm/bpm-processes/tasks/*")
                 .authenticated();

            // Role gates, in declaration order
            for (RoleGate gate : ROLE_GATES) {
                authz.requestMatchers(gate.patterns())
                     .hasAnyRole(gate.roles());
            }

            // Authenticated
            authz.requestMatchers(authenticatedPatterns())
                 .authenticated();

            // Deny all other requests
            authz.anyRequest()
                 .denyAll();
        });
    }

    /**
     * Builds the authenticated patterns, merging the static ones with any Ant patterns contributed by
     * hosted engines (e.g. the externalized OData engine) via {@link HostedEngineUris}. When no such
     * engine is on the classpath the result is just the static patterns.
     *
     * @return the authenticated request matcher patterns
     */
    private String[] authenticatedPatterns() {
        List<String> patterns = new ArrayList<>(List.of(AUTHENTICATED_PATTERNS));
        for (HostedEngineUris hostedEngineUris : BeanProvider.getBeans(HostedEngineUris.class)) {
            patterns.addAll(hostedEngineUris.securedAntPatterns());
        }
        return patterns.toArray(String[]::new);
    }

    private void applyCustomConfigurations(HttpSecurity http) throws Exception {
        Collection<CustomSecurityConfigurator> customConfigurators = BeanProvider.getBeans(CustomSecurityConfigurator.class);
        for (CustomSecurityConfigurator configurator : customConfigurators) {
            LOGGER.info("Applying custom security configurations using [{}]", configurator);
            configurator.configure(http);
        }
    }

}
