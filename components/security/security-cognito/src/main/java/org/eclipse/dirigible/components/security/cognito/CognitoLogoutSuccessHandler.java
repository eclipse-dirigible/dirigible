/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs the user out of AWS Cognito after the local session is cleared. Spring's default logout only
 * clears the local session, leaving the Cognito hosted-UI session alive - so the next login
 * silently re-authenticates the just-logged-out user without a credential prompt. Cognito does not
 * implement the standard OIDC end-session endpoint, so this handler redirects the browser to
 * Cognito's proprietary {@code /logout} endpoint ({@code logout_uri} must be registered as an
 * allowed sign-out URL on the Cognito app client).
 */
@Profile("cognito")
@Component
class CognitoLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final String LOGOUT_PATH = "/logout";

    private final String logoutEndpoint;
    private final String clientId;
    private final String logoutRedirectUri;

    CognitoLogoutSuccessHandler(@Value("${DIRIGIBLE_COGNITO_DOMAIN}") String cognitoDomain,
            @Value("${spring.security.oauth2.client.registration.cognito.client-id}") String clientId,
            @Value("${DIRIGIBLE_HOST}") String host) {
        this.logoutEndpoint = cognitoDomain + LOGOUT_PATH;
        this.clientId = clientId;
        this.logoutRedirectUri = host;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String logoutUrl = logoutEndpoint + "?client_id=" + encode(clientId) + "&logout_uri=" + encode(logoutRedirectUri);
        response.sendRedirect(logoutUrl);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
