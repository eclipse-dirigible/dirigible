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

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Matches a request made by script - fetch, XMLHttpRequest, a native application, a REST client -
 * as opposed to a browser navigation. The distinction decides what an unauthenticated request gets
 * back: a navigation is sent to the login, a programmatic request needs a 401 it can act on,
 * because a redirect to an HTML login page is useless to a script and, on the basic profile, a
 * {@code WWW-Authenticate: Basic} challenge pops the browser's own dialog before the script sees
 * it.
 *
 * <p>
 * The signals, in order: the {@code Sec-Fetch-Mode} fetch metadata every current browser sends (a
 * value other than {@code navigate}), the legacy {@code X-Requested-With: XMLHttpRequest} marker, a
 * bearer {@code Authorization} header (whoever sends a token expects a 401, never a redirect), and
 * an {@code Accept} header that prefers JSON - what axios and most REST clients send. A plain
 * {@code Accept: *&#47;*} does not count, so a client sending nothing specific keeps the redirect a
 * browser would get.
 */
public class ProgrammaticRequestMatcher implements RequestMatcher {

    private static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    private static final String NAVIGATE = "navigate";
    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";
    private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JSON_SUBTYPE_SUFFIX = "+json";

    /**
     * Matches.
     *
     * @param request the request
     * @return true for a programmatic request
     */
    @Override
    public boolean matches(HttpServletRequest request) {
        String secFetchMode = request.getHeader(SEC_FETCH_MODE_HEADER);
        if (secFetchMode != null) {
            return !NAVIGATE.equalsIgnoreCase(secFetchMode);
        }
        if (XML_HTTP_REQUEST.equalsIgnoreCase(request.getHeader(REQUESTED_WITH_HEADER))) {
            return true;
        }
        if (isBearerAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            return true;
        }
        return prefersJson(request.getHeader(HttpHeaders.ACCEPT));
    }

    /**
     * Whether an {@code Authorization} header value carries a bearer token.
     *
     * @param authorization the header value; may be {@code null}
     * @return true for the bearer scheme
     */
    public static boolean isBearerAuthorization(String authorization) {
        return authorization != null && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }

    /**
     * Whether the media types a client accepts most (the highest quality) include JSON and exclude
     * HTML.
     *
     * @param accept the {@code Accept} header value; may be {@code null}
     * @return true when JSON is preferred
     */
    static boolean prefersJson(String accept) {
        if (!StringUtils.hasText(accept)) {
            return false;
        }
        List<MediaType> mediaTypes;
        try {
            mediaTypes = MediaType.parseMediaTypes(accept);
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
        double highestQuality = mediaTypes.stream()
                                          .mapToDouble(MediaType::getQualityValue)
                                          .max()
                                          .orElse(0);
        List<MediaType> preferred = mediaTypes.stream()
                                              .filter(mediaType -> mediaType.getQualityValue() == highestQuality)
                                              .toList();
        if (preferred.stream()
                     .anyMatch(mediaType -> mediaType.isCompatibleWith(MediaType.TEXT_HTML) && !mediaType.isWildcardType())) {
            return false;
        }
        return preferred.stream()
                        .anyMatch(ProgrammaticRequestMatcher::isJson);
    }

    private static boolean isJson(MediaType mediaType) {
        if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
            return false;
        }
        return MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mediaType) || mediaType.getSubtype()
                                                                                      .endsWith(JSON_SUBTYPE_SUFFIX);
    }
}
