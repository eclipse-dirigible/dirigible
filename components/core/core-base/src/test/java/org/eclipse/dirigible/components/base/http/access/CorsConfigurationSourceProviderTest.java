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
import org.springframework.web.cors.CorsConfigurationSource;

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

    @Test
    void theStompEndpointIsLeftToItsOwnOriginCheck() {
        assertStompPathsHaveNoConfiguration();

        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com");

        assertStompPathsHaveNoConfiguration();
        assertNotNull(configuration(), "every other path keeps the platform's configuration");
    }

    private static void assertStompPathsHaveNoConfiguration() {
        CorsConfigurationSource source = CorsConfigurationSourceProvider.get();
        for (String path : List.of("/stomp", "/stomp/info", "/stomp/123/abc/xhr_streaming")) {
            assertNull(source.getCorsConfiguration(new MockHttpServletRequest("GET", path)),
                    "the STOMP endpoint answers its own CORS: " + path);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://app.example.com,*", "https://*", "*://*", "https://*:8080", "HTTPS://*", "https://*:[*]", "*://*:[*]",
            "https://*:*", "https://*:[8080]"})
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
        assertTrue(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*:[*]"), "a port pattern is no URI port, but hides nothing");
        assertTrue(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*:*"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*.example.com"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("https://*.example.com:[8080,8081]"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("https://app.example.com"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("capacitor://localhost"));
        assertFalse(CorsConfigurationSourceProvider.matchesEveryOrigin("not a uri"));
    }

    @Test
    void everyOriginWithoutCredentialsIsAllowed() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("*");

        assertEquals(List.of("*"), configuration().getAllowedOriginPatterns());
    }

    @Test
    void patternsNamingAHostAreToldFromTheRest() {
        assertTrue(CorsConfigurationSourceProvider.namesAHost("https://app.example.com"));
        assertTrue(CorsConfigurationSourceProvider.namesAHost("https://*.example.com"));
        assertTrue(CorsConfigurationSourceProvider.namesAHost("*://*.example.com"));
        assertTrue(CorsConfigurationSourceProvider.namesAHost("https://*.example.com:[8080]"));
        assertTrue(CorsConfigurationSourceProvider.namesAHost("capacitor://localhost"));
        assertTrue(CorsConfigurationSourceProvider.namesAHost("http://localhost:5173"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("*"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("https://*"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("HTTPS://*"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("https://*:[*]"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("https://*:*"));
        // Spring matches a pattern against the whole origin string, so these reach (nearly) every origin
        assertFalse(CorsConfigurationSourceProvider.namesAHost("h*"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("*/*"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("*.example.com"));
        assertFalse(CorsConfigurationSourceProvider.namesAHost("not a uri"));
    }

    @Test
    void theStompHandshakeGetsTheOriginsThatNameAHost() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue(
                "https://app.example.com, https://*.example.com:[8080], capacitor://localhost, *, https://*, h*, not a uri");

        assertEquals(List.of("https://app.example.com", "https://*.example.com:[8080]", "capacitor://localhost"),
                CorsConfigurationSourceProvider.stompOriginPatterns());
        assertEquals(7, CorsConfigurationSourceProvider.allowedOriginPatterns()
                                                       .size(),
                "the HTTP side keeps every configured pattern");
    }

    @Test
    void unconfiguredTheStompHandshakeGetsNothing() {
        assertTrue(CorsConfigurationSourceProvider.stompOriginPatterns()
                                                  .isEmpty());
    }

    @Test
    void credentialsForASchemelessPatternAreRefused() {
        // Spring compiles h* to h.* and full-matches it against the origin, so it admits
        // https://evil.example
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com,h*");
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), exception.getConfigKey());
        assertTrue(exception.getMessage()
                            .contains("h*"));
    }

    @Test
    void credentialsForANarrowPatternWithAPortListAreAllowed() {
        // one port only: the configuration value is split on commas, so a list of several ports never
        // arrives in one piece
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://*.example.com:[8080]");
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);

        CorsConfiguration configuration = configuration();

        assertEquals(List.of("https://*.example.com:[8080]"), configuration.getAllowedOriginPatterns());
        assertTrue(configuration.getAllowCredentials());
    }

    @Test
    void credentialsForAPatternThatCannotBeCheckedAreRefused() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://app.example.com,https://*.example.com:[8080");
        DirigibleConfig.CORS_ALLOW_CREDENTIALS.setBooleanValue(true);

        InvalidConfigException exception = assertThrows(InvalidConfigException.class, CorsConfigurationSourceProvider::get);

        assertEquals(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey(), exception.getConfigKey());
        assertTrue(exception.getMessage()
                            .contains("https://*.example.com:[8080"));
    }

    @Test
    void aPatternThatCannotBeCheckedIsAllowedWithoutCredentials() {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("not a uri");

        assertEquals(List.of("not a uri"), configuration().getAllowedOriginPatterns());
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
        assertTrue(CorsConfigurationSourceProvider.isTransportSecure("https://*.example.com:[*]"));
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
