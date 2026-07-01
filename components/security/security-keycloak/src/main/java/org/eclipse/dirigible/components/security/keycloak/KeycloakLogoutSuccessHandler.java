/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.keycloak;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Performs OIDC RP-initiated logout against Keycloak. Spring's default logout only clears the local
 * session, which leaves the Keycloak SSO session alive - so the next login silently
 * re-authenticates the just-logged-out user without a credential prompt. After the local session is
 * cleared this handler redirects the browser to Keycloak's end-session endpoint, terminating that
 * SSO session and sending the user back to the configured host.
 */
@Profile("keycloak")
@Component
class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final String END_SESSION_PATH = "/protocol/openid-connect/logout";

    private final String endSessionEndpoint;
    private final String clientId;
    private final String postLogoutRedirectUri;

    KeycloakLogoutSuccessHandler(@Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.client.registration.keycloak.client-id}") String clientId,
            @Value("${DIRIGIBLE_HOST}") String host) {
        this.endSessionEndpoint = issuerUri + END_SESSION_PATH;
        this.clientId = clientId;
        this.postLogoutRedirectUri = host;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        StringBuilder logoutUrl = new StringBuilder(endSessionEndpoint).append("?client_id=")
                                                                       .append(encode(clientId))
                                                                       .append("&post_logout_redirect_uri=")
                                                                       .append(encode(postLogoutRedirectUri));
        idTokenValue(authentication).ifPresent(idToken -> logoutUrl.append("&id_token_hint=")
                                                                   .append(encode(idToken)));
        response.sendRedirect(logoutUrl.toString());
    }

    private static Optional<String> idTokenValue(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token && token.getPrincipal() instanceof OidcUser oidcUser
                && oidcUser.getIdToken() != null) {
            return Optional.of(oidcUser.getIdToken()
                                       .getTokenValue());
        }
        return Optional.empty();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
