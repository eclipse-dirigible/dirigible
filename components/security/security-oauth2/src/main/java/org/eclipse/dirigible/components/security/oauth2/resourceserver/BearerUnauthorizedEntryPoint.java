/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.resourceserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The answer a programmatic client gets when it is not authenticated on an OAuth2 login profile: a
 * 401 with the {@code WWW-Authenticate: Bearer} challenge - carrying {@code error} and
 * {@code error_description} for a token that was presented and refused, as RFC 6750 prescribes -
 * and a JSON body in the shape of the platform's other error answers, so a script can read why
 * instead of following a redirect to an HTML login page. A token that is valid but lacks the scope
 * for the resource is answered 403, as the specification prescribes for {@code insufficient_scope}.
 *
 * <p>
 * The challenge is written here rather than by Spring's own entry point, which since Spring
 * Security 7.1 adds an RFC 9728 {@code resource_metadata} parameter pointing at the metadata
 * document its resource-server configurer serves at a well-known URL. The platform describes no
 * authorization server there, so the document tells a client nothing, and the parameter stays out.
 * The message names the OAuth2 error of a refused token (expired, wrong audience, ...) and nothing
 * else: the text of any other exception is not the client's business.
 */
public class BearerUnauthorizedEntryPoint implements AuthenticationEntryPoint {

    /** The message of an answer that is not about a refused token. */
    static final String AUTHENTICATION_REQUIRED = "Authentication required";

    private static final String BEARER = "Bearer";
    private static final Gson GSON = new Gson();

    /**
     * Commence.
     *
     * @param request the request
     * @param response the response
     * @param authException the reason the request is not authenticated
     * @throws IOException when the body cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        OAuth2Error error = authException instanceof OAuth2AuthenticationException oauth2Exception ? oauth2Exception.getError() : null;
        HttpStatus status = error != null && OAuth2ErrorCodes.INSUFFICIENT_SCOPE.equals(error.getErrorCode()) ? HttpStatus.FORBIDDEN
                : HttpStatus.UNAUTHORIZED;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now()
                                     .toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message",
                error != null && StringUtils.hasText(error.getDescription()) ? error.getDescription() : AUTHENTICATION_REQUIRED);
        body.put("path", request.getRequestURI());

        response.setStatus(status.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challengeOf(error));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(GSON.toJson(body));
    }

    /**
     * The RFC 6750 challenge: a bare scheme when no token was presented, the error of the token
     * otherwise.
     */
    private static String challengeOf(OAuth2Error error) {
        if (error == null) {
            return BEARER;
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        if (StringUtils.hasText(error.getErrorCode())) {
            parameters.put("error", error.getErrorCode());
        }
        if (StringUtils.hasText(error.getDescription())) {
            parameters.put("error_description", error.getDescription());
        }
        if (StringUtils.hasText(error.getUri())) {
            parameters.put("error_uri", error.getUri());
        }
        if (parameters.isEmpty()) {
            return BEARER;
        }
        return BEARER + " " + parameters.entrySet()
                                        .stream()
                                        .map(parameter -> parameter.getKey() + "=\"" + parameter.getValue()
                                                                                                .replace('"', '\'')
                                                + "\"")
                                        .collect(Collectors.joining(", "));
    }
}
