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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;

/**
 * CORS reaches a chain only once origins are configured: a CORS filter without a matching
 * configuration refuses every preflight, which is not what a chain without CORS did.
 */
class CorsSecurityConfiguratorTest {

    private final HttpSecurity http = mock(HttpSecurity.class);

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
    }

    @Test
    void unconfiguredLeavesTheChainAlone() throws Exception {
        new CorsSecurityConfigurator().configure(http);

        verify(http, never()).cors(any());
    }

    @Test
    void configuredOriginsEnableCors() throws Exception {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");
        when(http.getConfigurer(CorsConfigurer.class)).thenReturn(null);

        new CorsSecurityConfigurator().configure(http);

        verify(http).cors(any());
    }

    @Test
    void aChainThatConfiguresCorsItselfIsLeftAlone() throws Exception {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");
        when(http.getConfigurer(CorsConfigurer.class)).thenReturn(mock(CorsConfigurer.class));

        new CorsSecurityConfigurator().configure(http);

        // basic and snowflake resolve the same source through their own bean - a second application
        // would only validate and warn twice
        verify(http, never()).cors(any());
    }
}
