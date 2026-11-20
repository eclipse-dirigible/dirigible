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

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Hands the sign-in the identity provider refused to an application-owned login page
 * ({@code DIRIGIBLE_SECURITY_LOGIN_PAGE}): the failure redirects there with {@code ?error}, and the
 * page asks {@code GET /login/oauth2/failure} for the provider's {@code error} and the
 * {@code description} to show. The answer is given once; a second call, or a call without a refused
 * sign-in in the session, is a {@code 204}. The description is the provider's text - the page
 * escapes it like any other untrusted text.
 */
@RestController
class OAuth2LoginFailureEndpoint {

    /**
     * Takes the refused sign-in of this session.
     *
     * @param request the request
     * @return {@code {error, description?}}, or {@code 204} when there is none
     */
    @GetMapping(path = "/login/oauth2/failure", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> failure(HttpServletRequest request) {
        return OAuth2LoginFailure.take(request)
                                 .map(OAuth2LoginFailureEndpoint::body)
                                 .map(body -> ResponseEntity.ok()
                                                            .cacheControl(CacheControl.noStore())
                                                            .body(body))
                                 .orElseGet(() -> ResponseEntity.noContent()
                                                                .build());
    }

    private static Map<String, String> body(OAuth2LoginFailure failure) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", failure.error());
        if (failure.description() != null) {
            body.put("description", failure.description());
        }
        return body;
    }
}
