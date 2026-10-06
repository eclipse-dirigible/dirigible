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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refuses a cross-site request forgery: a state-changing request a browser sends from another site
 * with the user's ambient credentials.
 *
 * <p>
 * CSRF tokens are disabled on every security chain - the IDE and the generated applications send
 * none - so a page a logged in user visits could post a form to a generated create endpoint in that
 * user's name. Instead of a token this filter reads what the browser itself says about the request,
 * which needs no change in any client:
 * <ul>
 * <li>{@code Sec-Fetch-Site}, sent by every current browser and not settable by a script: anything
 * but {@code same-origin} or {@code none} (the user's own navigation) is cross-site - including
 * {@code same-site}, since a sibling subdomain may be another tenant's;</li>
 * <li>where it is missing, {@code Origin}: {@code null}, or a host other than the one the request
 * was addressed to ({@code X-Forwarded-Host}, else {@code Host}), is cross-site;</li>
 * <li>without either header the request does not come from a browser page and passes - a
 * server-side client sends neither.</li>
 * </ul>
 * Only requests carrying an ambient credential are concerned - a session id, or HTTP authentication
 * other than a bearer token, which a browser may replay on its own. A bearer token is never
 * ambient: a page can only send it by script, and a script reaches another origin only through
 * CORS. An origin configured in {@link DirigibleConfig#CORS_ALLOWED_ORIGINS} by host is the
 * operator's explicit trust decision and passes; a pattern naming no host (the wildcard) does not,
 * as it would trust every site.
 */
public class CrossSiteRequestFilter extends OncePerRequestFilter {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossSiteRequestFilter.class);

    static final String SEC_FETCH_SITE = "Sec-Fetch-Site";
    static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> SAME_ORIGIN_FETCH_SITES = Set.of("same-origin", "none");
    private static final String BEARER_PREFIX = "bearer ";
    private static final String NULL_ORIGIN = "null";

    /** The configured origins that name a host, null when none is configured. */
    private final CorsConfiguration trustedOrigins;

    public CrossSiteRequestFilter() {
        List<String> patterns = CorsConfigurationSourceProvider.allowedOriginPatterns()
                                                               .stream()
                                                               .filter(CorsConfigurationSourceProvider::namesAHost)
                                                               .toList();
        if (patterns.isEmpty()) {
            this.trustedOrigins = null;
        } else {
            this.trustedOrigins = new CorsConfiguration();
            this.trustedOrigins.setAllowedOriginPatterns(patterns);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod()
                                         .toUpperCase(Locale.ROOT))
                || !carriesAmbientCredentials(request) || !isCrossSite(request) || isTrusted(request.getHeader(HttpHeaders.ORIGIN))) {
            filterChain.doFilter(request, response);
            return;
        }
        LOGGER.warn(
                "Refused a cross-site [{}] on [{}] from origin [{}] ({} [{}]) carrying the user's session or browser credentials."
                        + " A page of another site cannot act in a logged in user's name; a trusted front end belongs in [{}], a"
                        + " server-side client authenticates without a browser.",
                forLog(request.getMethod()), forLog(request.getRequestURI()), forLog(request.getHeader(HttpHeaders.ORIGIN)), SEC_FETCH_SITE,
                forLog(request.getHeader(SEC_FETCH_SITE)), DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter()
                .write("Cross-site request refused");
    }

    /**
     * Whether the request carries a credential the browser attaches on its own: a session id, or HTTP
     * authentication that is not a bearer token.
     */
    static boolean carriesAmbientCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return false;
        }
        return request.getRequestedSessionId() != null || authorization != null;
    }

    /** Whether the browser says the request was sent by a page of another origin. */
    static boolean isCrossSite(HttpServletRequest request) {
        String fetchSite = request.getHeader(SEC_FETCH_SITE);
        if (fetchSite != null) {
            return !SAME_ORIGIN_FETCH_SITES.contains(fetchSite.trim()
                                                              .toLowerCase(Locale.ROOT));
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null) {
            return false;
        }
        String originHost = hostOf(origin);
        String requestHost = requestHost(request);
        return originHost == null || requestHost == null || !originHost.equalsIgnoreCase(requestHost);
    }

    private boolean isTrusted(String origin) {
        return trustedOrigins != null && origin != null && !NULL_ORIGIN.equalsIgnoreCase(origin)
                && trustedOrigins.checkOrigin(origin) != null;
    }

    /**
     * The host the request was addressed to, before any proxy: {@code X-Forwarded-Host}, else
     * {@code Host}.
     */
    private static String requestHost(HttpServletRequest request) {
        String forwarded = request.getHeader(X_FORWARDED_HOST);
        String host = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0] : request.getHeader(HttpHeaders.HOST);
        if (host == null || host.isBlank()) {
            host = request.getServerName();
        }
        return hostOf("http://" + host.trim());
    }

    /**
     * A request value as it may appear in the log: an anonymous client chooses these, so control
     * characters, which could forge or garble log lines, are replaced.
     */
    static String forLog(String value) {
        return value == null ? null : value.replaceAll("\\p{Cntrl}", "_");
    }

    /** The host of an origin or authority, without its port; null when it is none. */
    private static String hostOf(String origin) {
        if (NULL_ORIGIN.equalsIgnoreCase(origin.trim())) {
            return null;
        }
        try {
            return new URI(origin.trim()).getHost();
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
