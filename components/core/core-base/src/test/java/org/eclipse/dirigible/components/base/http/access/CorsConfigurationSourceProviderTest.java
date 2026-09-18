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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Unconfigured, the historical wildcard shape stays - without credentials. Configured, the origins
 * get what the configuration grants, and a combination that cannot be safe is refused at boot.
 */
class CorsConfigurationSourceProviderTest {

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
        Configuration.remove(DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey());
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_METHODS.getKey());
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_HEADERS.getKey());
        Configuration.remove(DirigibleConfig.CORS_EXPOSED_HEADERS.getKey());
        Configuration.remove(DirigibleConfig.CORS_MAX_AGE.getKey());
    }

    @Test
    void unconfiguredKeepsTheWildcardShapeWithoutCredentials() {
        assertFalse(CorsConfigurationSourceProvider.isConfigured());

        CorsConfiguration configuration = configuration();

        assertEquals(List.of("*"), configuration.getAllowedOriginPatterns());
        assertFalse(configuration.getAllowCredentials(), "cookies must not be accepted from every origin");
        assertTrue(configuration.getAllowedHeaders()
                                .contains("Authorization"));
        assertEquals(List.of("HEAD", "DELETE", "GET", "POST", "PATCH", "PUT"), configuration.getAllowedMethods());
        assertNull(configuration.getMaxAge());
    }

    @Test
    void configuredOriginsGetTheDefaultsOfTheOtherKeys() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue(" https://app.example.com , capacitor://localhost ");

        assertTrue(CorsConfigurationSourceProvider.isConfigured());
        assertEquals(List.of("https://app.example.com", "capacitor://localhost"), CorsConfigurationSourceProvider.allowedOriginPatterns());

        CorsConfiguration configuration = configuration();
        assertEquals(List.of("https://app.example.com", "capacitor://localhost"), configuration.getAllowedOriginPatterns());
        assertFalse(configuration.getAllowCredentials());
        assertEquals(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), configuration.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"), configuration.getAllowedHeaders());
        assertEquals(List.of("Content-Disposition"), configuration.getExposedHeaders());
        assertEquals(3600L, configuration.getMaxAge());
    }

    @Test
    void everyKeyIsHonoured() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://*.example.com");
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);
        DirigibleConfig.CORS_ALLOWED_METHODS.setStringValue("get, post");
        DirigibleConfig.CORS_ALLOWED_HEADERS.setStringValue("Content-Type,X-Custom");
        DirigibleConfig.CORS_EXPOSED_HEADERS.setStringValue("X-Total-Count, Content-Disposition");
        DirigibleConfig.CORS_MAX_AGE.setIntValue(600);

        CorsConfiguration configuration = configuration();

        assertEquals(List.of("https://*.example.com"), configuration.getAllowedOriginPatterns());
        assertTrue(configuration.getAllowCredentials());
        assertEquals(List.of("GET", "POST"), configuration.getAllowedMethods());
        assertEquals(List.of("Content-Type", "X-Custom"), configuration.getAllowedHeaders());
        assertEquals(List.of("X-Total-Count", "Content-Disposition"), configuration.getExposedHeaders());
        assertEquals(600L, configuration.getMaxAge());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://app.example.com,*", "https://*", "*://*", "https://*:8080", "HTTPS://*"})
    void credentialsForEveryOriginAreRefused(String origins) {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue(origins);
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey(), exception.getConfigKey());
    }

    @Test
    void everyOriginPatternsAreToldFromNarrowOnes() {
        assertTrue(CorsConfigurationSourceProvider.matchesEveryOrigin("*"));
        assertTrue(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*"));
        assertTrue(CorsConfigurationSourceProvider.matchesEveryOrigin("*://*"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*.example.com"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("https://app.example.com"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("capacitor://localhost"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("not a uri"));
    }

    @Test
    void everyOriginWithoutCredentialsIsAllowed() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("*");

        assertEquals(List.of("*"), configuration().getAllowedOriginPatterns());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "NULL", "https://app.example.com,null"})
    void theNullOriginIsRefused(String origins) {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue(origins);

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), exception.getConfigKey());
    }

    @Test
    void credentialsWithEveryRequestHeaderAreRefused() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);
        DirigibleConfig.CORS_ALLOWED_HEADERS.setStringValue("*");

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOWED_HEADERS.getKey(), exception.getConfigKey());
    }

    @Test
    void anUnsupportedMethodIsRefused() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");
        DirigibleConfig.CORS_ALLOWED_METHODS.setStringValue("GET,TRACE");

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOWED_METHODS.getKey(), exception.getConfigKey());
    }

    @Test
    void transportSecurityIsRecognized() {
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("https://app.example.com"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("https://*.example.com"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("tauri://localhost"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("capacitor://localhost"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("http://localhost:5173"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("http://app.localhost"));
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("http://127.0.0.1:3000"));
        assertFalse(CorsConfigurationSourceProvider.isTransportSecure("http://app.example.com"));
        assertFalse(CorsConfigurationSourceProvider.isTransportSecure("*"));
        assertFalse(CorsConfigurationSourceProvider.isTransportSecure("not a uri"));
    }

    private static CorsConfiguration configuration() {
        CorsConfiguration configuration = CorsConfigurationSourceProvider.get()
                                                                         .getCorsConfiguration(
                                                                                 new MockHttpServletRequest("GET", "/services/js/x"));
        assertNotNull(configuration, "the configuration must apply to every path");
        return configuration;
    }
}
