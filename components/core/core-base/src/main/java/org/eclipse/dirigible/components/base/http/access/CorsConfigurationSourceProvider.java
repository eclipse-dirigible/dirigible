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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The CORS configuration of the platform, built from {@link DirigibleConfig#CORS_ALLOWED_ORIGINS}
 * and its siblings.
 *
 * <p>
 * Unconfigured, the chains that always answered cross-origin requests (basic, snowflake) keep their
 * historical shape - every origin, the usual headers and methods - with one change: cookies are no
 * longer accepted cross-site. A wildcard origin combined with credentials lets any page a logged in
 * user happens to visit call the platform in that user's name, and CSRF tokens are disabled on
 * every chain, so nothing else would stop such a call.
 *
 * <p>
 * Configured, the listed origins get exactly what the configuration grants, and the OAuth2 login
 * chains, which have no CORS otherwise, enable it through {@link CorsSecurityConfigurator}. A
 * configuration that cannot be safe is refused at boot rather than served.
 */
public class CorsConfigurationSourceProvider {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsConfigurationSourceProvider.class);

    private static final String WILDCARD = "*";
    private static final String NULL_ORIGIN = "null";
    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final Set<String> NATIVE_SHELL_SCHEMES = Set.of("tauri", "capacitor", "ionic");
    private static final long MAX_AGE_WARNING_THRESHOLD_SECONDS = 86_400;

    private CorsConfigurationSourceProvider() {
        // static access only
    }

    /**
     * Whether origins are configured, i.e. whether cross-origin access is a deliberate deployment
     * decision rather than the historical default.
     *
     * @return true when at least one origin is configured
     */
    public static boolean isConfigured() {
        return !allowedOriginPatterns().isEmpty();
    }

    /**
     * The configured origin patterns.
     *
     * @return the patterns, empty when none is configured
     */
    public static List<String> allowedOriginPatterns() {
        return DirigibleConfig.CORS_ALLOWED_ORIGINS.getListValue();
    }

    /**
     * Builds the configuration source for every path.
     *
     * @return the configuration source
     * @throws InvalidConfigException when the configured combination cannot be safe
     */
    public static CorsConfigurationSource get() {
        CorsConfiguration configuration = isConfigured() ? configured() : unconfigured();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private static CorsConfiguration unconfigured() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(WILDCARD));
        configuration.setAllowCredentials(false);
        configuration.setAllowedHeaders(
                Arrays.asList("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method",
                        "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization"));
        configuration.setExposedHeaders(
                Arrays.asList("Access-Control-Allow-Headers", "Access-Control-Allow-Origin", "Access-Control-Request-Method",
                        "Access-Control-Request-Headers", "Origin", "Cache-Control", "Content-Type", "Authorization"));
        configuration.setAllowedMethods(Arrays.asList("HEAD", "DELETE", "GET", "POST", "PATCH", "PUT"));
        return configuration;
    }

    private static CorsConfiguration configured() {
        List<String> origins = allowedOriginPatterns();
        boolean allowCredentials = DirigibleConfig.CORS_ALLOW_CREDENTIALS.getBooleanValue();
        List<String> methods = DirigibleConfig.CORS_ALLOWED_METHODS.getListValue()
                                                                   .stream()
                                                                   .map(method -> method.toUpperCase(Locale.ROOT))
                                                                   .toList();
        List<String> headers = DirigibleConfig.CORS_ALLOWED_HEADERS.getListValue();
        List<String> exposedHeaders = DirigibleConfig.CORS_EXPOSED_HEADERS.getListValue();
        long maxAge = DirigibleConfig.CORS_MAX_AGE.getIntValue();

        refuseUnsafe(origins, allowCredentials, methods, headers);
        warnAboutRisks(origins, allowCredentials, maxAge);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setAllowedMethods(methods);
        configuration.setAllowedHeaders(headers);
        configuration.setExposedHeaders(exposedHeaders);
        configuration.setMaxAge(maxAge);
        return configuration;
    }

    private static void refuseUnsafe(List<String> origins, boolean allowCredentials, List<String> methods, List<String> headers) {
        if (origins.stream()
                   .anyMatch(origin -> NULL_ORIGIN.equalsIgnoreCase(origin))) {
            throw new InvalidConfigException("The [null] origin cannot be allowed: it is what sandboxed frames, file pages and redirects"
                    + " present, so it identifies no site at all", DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
        }
        if (allowCredentials && origins.contains(WILDCARD)) {
            throw new InvalidConfigException(
                    "Credentials cannot be allowed for every origin [" + WILDCARD + "]: any site a logged in"
                            + " user visits could then call the platform with that user's session",
                    DirigibleConfig.CORS_ALLOW_CREDENTIALS.getKey());
        }
        if (allowCredentials && headers.contains(WILDCARD)) {
            throw new InvalidConfigException("Credentials cannot be allowed together with every request header [" + WILDCARD + "]",
                    DirigibleConfig.CORS_ALLOWED_HEADERS.getKey());
        }
        List<String> unsupportedMethods = methods.stream()
                                                 .filter(method -> !SUPPORTED_METHODS.contains(method))
                                                 .toList();
        if (!unsupportedMethods.isEmpty()) {
            throw new InvalidConfigException("Unsupported HTTP methods " + unsupportedMethods + ", supported are " + SUPPORTED_METHODS,
                    DirigibleConfig.CORS_ALLOWED_METHODS.getKey());
        }
    }

    private static void warnAboutRisks(List<String> origins, boolean allowCredentials, long maxAge) {
        if (allowCredentials) {
            LOGGER.warn("Cross-origin requests from {} may carry the session cookie. CSRF tokens are disabled on every security chain, so"
                    + " these origins are trusted with the sessions of logged in users.", origins);
        }
        List<String> insecureOrigins = origins.stream()
                                              .filter(origin -> !isTransportSecure(origin))
                                              .toList();
        if (!insecureOrigins.isEmpty()) {
            LOGGER.warn("Cross-origin requests are accepted from origins without transport security: {}. Anything on the path can"
                    + " read the tokens such a page sends.", insecureOrigins);
        }
        if (maxAge > MAX_AGE_WARNING_THRESHOLD_SECONDS) {
            LOGGER.warn("Browsers may cache preflight answers for [{}] seconds. A tightened configuration reaches them only after"
                    + " that time.", maxAge);
        }
        if (DirigibleConfig.TRIAL_ENABLED.getBooleanValue()) {
            LOGGER.warn("Trial mode grants every role to every user, and the origins {} may now call the platform cross-origin.", origins);
        }
    }

    /**
     * Whether an origin (pattern) reaches the platform over a channel nobody on the path can read:
     * https, a native application shell scheme, or plain http on the local machine only.
     */
    static boolean isTransportSecure(String origin) {
        if (WILDCARD.equals(origin)) {
            return false;
        }
        URI uri;
        try {
            // a pattern host such as *.example.com is not a valid URI host - substitute before parsing
            uri = new URI(origin.replace(WILDCARD, "wildcard"));
        } catch (URISyntaxException ex) {
            LOGGER.debug("Origin [{}] is not a URI and cannot be checked for transport security", origin, ex);
            return false;
        }
        String scheme = uri.getScheme() == null ? ""
                : uri.getScheme()
                     .toLowerCase(Locale.ROOT);
        if ("https".equals(scheme) || NATIVE_SHELL_SCHEMES.contains(scheme)) {
            return true;
        }
        String host = uri.getHost() == null ? ""
                : uri.getHost()
                     .toLowerCase(Locale.ROOT);
        return "http".equals(scheme)
                && ("localhost".equals(host) || host.endsWith(".localhost") || "127.0.0.1".equals(host) || "[::1]".equals(host));
    }
}
