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

import java.io.IOException;
import java.util.Set;
import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The failure handler of the OAuth2 login callback: logs the refusal once, keeps the provider's
 * answer for the login page to read once ({@link OAuth2LoginFailure}), and then redirects to the
 * login page with {@code ?error} as Spring Security's default does.
 *
 * <p>
 * It runs only for the callback, which Spring Security processes only when its {@code state}
 * matched an authorization request of this session - so what is kept is the answer to this person's
 * own sign-in, never text a link brought in.
 */
class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    /**
     * The codes the client raises itself about a callback or a response it could not use. Their
     * descriptions are the client's diagnostics (and may quote a raw response body), not words the
     * provider addressed to the person, so they are logged and never shown.
     */
    private static final Set<String> CLIENT_DIAGNOSTICS =
            Set.of("authorization_request_not_found", "client_registration_not_found", "invalid_state_parameter", "invalid_token_response",
                    "invalid_user_info_response", "missing_user_info_uri", "missing_user_name_attribute", "invalid_id_token",
                    "invalid_nonce", "missing_signature_verifier", "invalid_key", "invalid_algorithm");

    private final OAuth2LoginFailureMessages messages;

    /**
     * Creates the handler.
     *
     * @param failureUrl the login page with {@code ?error}
     * @param messages turns the provider's error into the sentence the person reads
     */
    OAuth2LoginFailureHandler(String failureUrl, OAuth2LoginFailureMessages messages) {
        super(failureUrl);
        this.messages = messages;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        // the callback's path and the provider's text both come from the request: a control character in
        // either must not forge a log entry
        String callback = LogSanitizer.sanitize(request.getRequestURI());
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            OAuth2Error error = oauth2Exception.getError();
            LOGGER.warn("The identity provider refused a sign-in at [{}]: error [{}], description [{}]", callback,
                    LogSanitizer.sanitize(error.getErrorCode()), LogSanitizer.sanitize(error.getDescription()));
            LOGGER.debug("The refused sign-in at [{}]", callback, exception);
            new OAuth2LoginFailure(error.getErrorCode(), messageOf(error)).store(request);
        } else {
            LOGGER.warn("A sign-in at [{}] failed: {}", callback, LogSanitizer.sanitize(exception.getMessage()), exception);
        }
        super.onAuthenticationFailure(request, response, exception);
    }

    private String messageOf(OAuth2Error error) {
        if (CLIENT_DIAGNOSTICS.contains(error.getErrorCode())) {
            return null;
        }
        String message = messages.messageOf(error);
        return message == null || message.isBlank() ? null : message.strip();
    }
}
