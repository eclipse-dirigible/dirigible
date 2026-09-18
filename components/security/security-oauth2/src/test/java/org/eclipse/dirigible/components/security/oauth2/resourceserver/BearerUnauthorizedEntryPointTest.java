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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A script that is not authenticated gets a 401 with the bearer challenge and a JSON body in the
 * shape of the platform's other error answers; the message names the OAuth2 error of a refused
 * token and nothing of any other exception.
 */
class BearerUnauthorizedEntryPointTest {

    private final BearerUnauthorizedEntryPoint entryPoint = new BearerUnauthorizedEntryPoint();

    @Test
    void anUnauthenticatedRequestGetsTheChallengeAndAGenericMessage() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/ide/workspaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication is required"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertTrue(response.getContentType()
                           .startsWith(MediaType.APPLICATION_JSON_VALUE));
        JsonObject body = JsonParser.parseString(response.getContentAsString())
                                    .getAsJsonObject();
        assertEquals(401, body.get("status")
                              .getAsInt());
        assertEquals("Unauthorized", body.get("error")
                                         .getAsString());
        assertEquals(BearerUnauthorizedEntryPoint.AUTHENTICATION_REQUIRED, body.get("message")
                                                                               .getAsString());
        assertEquals("/services/ide/workspaces", body.get("path")
                                                     .getAsString());
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void aRefusedTokenGetsItsOAuth2ErrorInHeaderAndBody() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/js/app/api.mjs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InvalidBearerTokenException("The token is not intended for this client"));

        assertEquals(401, response.getStatus());
        String challenge = response.getHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertTrue(challenge.startsWith("Bearer "), challenge);
        assertTrue(challenge.contains("error=\"invalid_token\""), challenge);
        assertTrue(challenge.contains("The token is not intended for this client"), challenge);
        JsonObject body = JsonParser.parseString(response.getContentAsString())
                                    .getAsJsonObject();
        assertEquals("The token is not intended for this client", body.get("message")
                                                                      .getAsString());
    }
}
