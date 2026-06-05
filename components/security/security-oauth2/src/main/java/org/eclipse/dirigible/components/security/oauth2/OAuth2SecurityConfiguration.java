/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2;

import org.eclipse.dirigible.components.base.http.access.HttpSecurityURIConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The Class OAuth2SecurityConfiguration.
 */
@Profile("github")
@Configuration
public class OAuth2SecurityConfiguration {

    /** The JWKS endpoint backing the resource-server (Bearer) JWT decoder. */
    private final String jwkSetUri;

    public OAuth2SecurityConfiguration(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
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
    SecurityFilterChain filterChain(HttpSecurity http, HttpSecurityURIConfigurator httpSecurityURIConfigurator) throws Exception {
        http//
            .authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOpts -> frameOpts.disable()))
            .oauth2Client(Customizer.withDefaults())
            .oauth2Login(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));

        httpSecurityURIConfigurator.configure(http);

        return http.build();
    }

    /**
     * Builds the resource-server JWT decoder explicitly from the configured JWKS endpoint (default JWS
     * algorithm RS256).
     *
     * <p>
     * This must be pinned on the configurer rather than relying on a {@link JwtDecoder} bean: the
     * embedded Spring Authorization Server publishes its own {@code JwtDecoder} (backed by its
     * in-memory keys), and the Spring Boot OAuth2 resource-server auto-configuration is not on the
     * classpath, so {@code getBean(JwtDecoder.class)} would otherwise resolve the authorization
     * server's decoder and reject every token. Validation (signature/expiry against
     * {@code jwk-set-uri}) is unchanged.
     *
     * @return the JWKS-backed JWT decoder
     */
    private JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                               .build();
    }
}
