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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Unit tests for {@link OAuth2LoginFailureHandler} - what of a refusal is kept for the login page.
 */
class OAuth2LoginFailureHandlerTest {

    private final OAuth2LoginFailureHandler handler =
            new OAuth2LoginFailureHandler("/login?error", OAuth2LoginFailureMessages.descriptions());

    @Test
    void keepsTheProvidersErrorAndDescriptionAndRedirects() throws Exception {
        MockHttpServletRequest request = callback();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", "Account is disabled", null)));

        assertEquals("/login?error", response.getRedirectedUrl());
        OAuth2LoginFailure failure = OAuth2LoginFailure.take(request)
                                                       .orElseThrow();
        assertEquals("invalid_grant", failure.error());
        assertEquals("Account is disabled", failure.description());
        assertTrue(OAuth2LoginFailure.take(request)
                                     .isEmpty());
    }

    @Test
    void neverShowsTheClientsOwnDiagnostics() throws Exception {
        MockHttpServletRequest request = callback();

        handler.onAuthenticationFailure(request, new MockHttpServletResponse(),
                new OAuth2AuthenticationException(new OAuth2Error("invalid_token_response",
                        "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: 502 <html>", null)));

        OAuth2LoginFailure failure = OAuth2LoginFailure.take(request)
                                                       .orElseThrow();
        assertEquals("invalid_token_response", failure.error());
        assertNull(failure.description());
    }

    @Test
    void cutsALongDescription() throws Exception {
        MockHttpServletRequest request = callback();

        handler.onAuthenticationFailure(request, new MockHttpServletResponse(),
                new OAuth2AuthenticationException(new OAuth2Error("access_denied", "x".repeat(5000), null)));

        String description = OAuth2LoginFailure.take(request)
                                               .orElseThrow()
                                               .description();
        assertEquals(OAuth2LoginFailure.MAX_LENGTH, description.length());
        assertTrue(description.endsWith("…"));
    }

    @Test
    void keepsNothingForAFailureThatIsNotTheProviders() throws Exception {
        MockHttpServletRequest request = callback();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals("/login?error", response.getRedirectedUrl());
        assertTrue(OAuth2LoginFailure.take(request)
                                     .isEmpty());
    }

    private static MockHttpServletRequest callback() {
        return new MockHttpServletRequest("GET", "/login/oauth2/code/test");
    }
}
