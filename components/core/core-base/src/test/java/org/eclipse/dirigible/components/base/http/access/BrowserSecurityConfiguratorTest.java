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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * A chain the configurator reaches answers with the browser security headers and refuses a forged
 * cross-site post, whatever the chain itself configured.
 */
@SpringJUnitWebConfig(BrowserSecurityConfiguratorTest.Config.class)
class BrowserSecurityConfiguratorTest {

    @EnableWebMvc
    @EnableWebSecurity
    static class Config {

        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            // the shape of every platform chain: tokens off, frames left open
            http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(options -> options.disable()))
                .authorizeHttpRequests(authz -> authz.anyRequest()
                                                     .permitAll());
            new BrowserSecurityConfigurator().configure(http);
            return http.build();
        }

        @Bean
        Endpoint endpoint() {
            return new Endpoint();
        }
    }

    @RestController
    static class Endpoint {

        @PostMapping("/services/ts/app/gen/api/Invoice")
        String create() {
            return "created";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                             .apply(springSecurity())
                             .build();
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.SECURITY_FRAME_OPTIONS.getKey());
        Configuration.remove(DirigibleConfig.SECURITY_HSTS.getKey());
        Configuration.remove(DirigibleConfig.SECURITY_REFERRER_POLICY.getKey());
    }

    @Test
    void everyResponseCarriesTheBrowserSecurityHeaders() throws Exception {
        mvc.perform(get("/"))
           .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
           .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
           .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void hstsIsSentOnSecureRequests() throws Exception {
        mvc.perform(get("/").secure(true))
           .andExpect(header().exists("Strict-Transport-Security"));
        mvc.perform(get("/"))
           .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void aForgedCrossSitePostIsRefused() throws Exception {
        mvc.perform(post("/services/ts/app/gen/api/Invoice").header("Sec-Fetch-Site", "cross-site")
                                                            .header("Origin", "https://evil.example.org")
                                                            .with(BrowserSecurityConfiguratorTest::withSession))
           .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void aSameOriginPostReachesTheEndpoint() throws Exception {
        mvc.perform(post("/services/ts/app/gen/api/Invoice").header("Sec-Fetch-Site", "same-origin")
                                                            .with(BrowserSecurityConfiguratorTest::withSession))
           .andExpect(status().isOk());
    }

    @Test
    void anUnsupportedFrameOptionIsRefused() {
        DirigibleConfig.SECURITY_FRAME_OPTIONS.setStringValue("ALLOW-FROM https://example.org");

        assertThrows(InvalidConfigException.class, () -> new BrowserSecurityConfigurator().configureHeaders(headers()));
    }

    @Test
    void anUnsupportedHstsModeIsRefused() {
        DirigibleConfig.SECURITY_HSTS.setStringValue("sometimes");

        assertThrows(InvalidConfigException.class, () -> new BrowserSecurityConfigurator().configureHeaders(headers()));
    }

    @Test
    void anUnsupportedReferrerPolicyIsRefused() {
        DirigibleConfig.SECURITY_REFERRER_POLICY.setStringValue("whatever");

        assertThrows(InvalidConfigException.class, () -> new BrowserSecurityConfigurator().configureHeaders(headers()));
    }

    private static MockHttpServletRequest withSession(MockHttpServletRequest request) {
        request.setRequestedSessionId("4A2C1F");
        return request;
    }

    @SuppressWarnings("unchecked")
    private static HeadersConfigurer<HttpSecurity> headers() {
        return mock(HeadersConfigurer.class);
    }
}
