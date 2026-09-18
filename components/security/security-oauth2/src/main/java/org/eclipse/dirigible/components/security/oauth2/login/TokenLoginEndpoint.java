/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import java.time.Instant;
import java.util.Map;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSettings;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.ResourceServerJwtSupport;
import org.eclipse.dirigible.components.security.oauth2.resourceserver.TokenKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exchanges a bearer ID token for the standard platform session: {@code POST /login/token} with the
 * token in the {@code Authorization} header answers with the session cookie, so a frontend that
 * authenticates with tokens can open the platform's cookie-based surfaces (the IDE, a generated
 * application) as the same user. On profiles without bearer support (basic, github, ...) the
 * endpoint answers 404.
 *
 * <p>
 * The endpoint never reads or validates the token itself. The resource server has already done that
 * with the rules of the profile and put the resulting identity into the security context, so the
 * session gets exactly the name and the roles the bearer request had. Only an ID token qualifies:
 * an access token is the grant of a client, not the identity of a user, and never carried enough to
 * become a session. The session is filed under the profile's default client registration - the one
 * whose identity provider the resource server validates tokens against, so no other could match.
 *
 * <p>
 * Cross-site request forgery cannot reach it: a browser adds no {@code Authorization} header of its
 * own accord, so a request carrying only cookies is answered 401 and mints nothing.
 */
@RestController
class TokenLoginEndpoint {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenLoginEndpoint.class);

    private final ObjectProvider<ResourceServerJwtSupport> jwtSupport;
    private final NativeLoginSessionInitializer sessionInitializer;

    TokenLoginEndpoint(ObjectProvider<ResourceServerJwtSupport> jwtSupport, NativeLoginSessionInitializer sessionInitializer) {
        this.jwtSupport = jwtSupport;
        this.sessionInitializer = sessionInitializer;
    }

    /**
     * Mints the platform session for the user the bearer ID token of the request identifies.
     *
     * @param request the request
     * @param response the response
     * @return {@code AUTHENTICATED} with the instant the session ends, {@code UNAUTHENTICATED} (401)
     *         without a validated bearer token, {@code ID_TOKEN_REQUIRED} (403) for an access token
     */
    @PostMapping(path = "/login/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request, HttpServletResponse response) {
        ResourceServerJwtSettings settings = requireSupport().settings();
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken idTokenAuthentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                 .body(Map.of("outcome", "UNAUTHENTICATED"));
        }
        TokenKind kind = settings.kindOf(idTokenAuthentication.getToken())
                                 .orElse(null);
        if (kind != TokenKind.ID) {
            LOGGER.debug("Refusing to mint a session from a bearer token of kind [{}] for [{}]", kind, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("outcome", "ID_TOKEN_REQUIRED"));
        }
        Instant expiresAt = sessionInitializer.establishSession(settings.defaultRegistrationId(), idTokenAuthentication,
                settings.principalClaim(), request, response);
        return ResponseEntity.ok(Map.of("outcome", "AUTHENTICATED", "expiresAt", expiresAt.toString()));
    }

    private ResourceServerJwtSupport requireSupport() {
        ResourceServerJwtSupport support = jwtSupport.getIfAvailable();
        if (support == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token login is not supported by the active security profile");
        }
        return support;
    }
}
