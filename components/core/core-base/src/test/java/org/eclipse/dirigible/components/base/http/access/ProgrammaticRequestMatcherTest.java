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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * A script gets a 401 it can act on, a navigation gets the login: the matcher decides which is
 * which from the fetch metadata, the legacy XHR marker, a bearer token, or an Accept header that
 * prefers JSON.
 */
class ProgrammaticRequestMatcherTest {

    private final ProgrammaticRequestMatcher matcher = new ProgrammaticRequestMatcher();

    @ParameterizedTest
    @ValueSource(strings = {"cors", "no-cors", "same-origin", "websocket"})
    void fetchMetadataOtherThanNavigateIsProgrammatic(String secFetchMode) {
        MockHttpServletRequest request = request();
        request.addHeader("Sec-Fetch-Mode", secFetchMode);
        // a browser navigation accepts HTML - the metadata still wins
        request.addHeader("Accept", "text/html,application/xhtml+xml");

        assertTrue(matcher.matches(request));
    }

    @Test
    void aNavigationIsNotProgrammaticWhateverElseItCarries() {
        MockHttpServletRequest request = request();
        request.addHeader("Sec-Fetch-Mode", "navigate");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Accept", "application/json");

        assertFalse(matcher.matches(request));
    }

    @Test
    void theLegacyXhrMarkerIsProgrammatic() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Requested-With", "xmlhttprequest");

        assertTrue(matcher.matches(request));
    }

    @Test
    void aBearerTokenIsProgrammatic() {
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "bearer eyJ...");
        request.addHeader("Accept", "text/html");

        assertTrue(matcher.matches(request));
    }

    @Test
    void basicCredentialsAreNotProgrammaticByThemselves() {
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        assertFalse(matcher.matches(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json, text/plain, */*", "application/problem+json",
            "application/json;q=1, text/html;q=0.9", "application/vnd.api+json, application/json"})
    void anAcceptHeaderPreferringJsonIsProgrammatic(String accept) {
        MockHttpServletRequest request = request();
        request.addHeader("Accept", accept);

        assertTrue(matcher.matches(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8", "*/*",
            "text/html, application/json", "application/json;q=0.8, text/html", "text/csv", "application/xml", "text/*", "not a media type",
            "application/json;q=abc"})
    void anythingElseIsNotProgrammatic(String accept) {
        MockHttpServletRequest request = request();
        request.addHeader("Accept", accept);

        assertFalse(matcher.matches(request));
    }

    @Test
    void aRequestWithoutAnyMarkerIsNotProgrammatic() {
        assertFalse(matcher.matches(request()));
    }

    @Test
    void theBrowserOnlyMatcherKeepsTheBrowserSignals() {
        ProgrammaticRequestMatcher browserScripts = ProgrammaticRequestMatcher.ofBrowserScripts();
        MockHttpServletRequest fetch = request();
        fetch.addHeader("Sec-Fetch-Mode", "cors");
        MockHttpServletRequest xhr = request();
        xhr.addHeader("X-Requested-With", "XMLHttpRequest");

        assertTrue(browserScripts.matches(fetch));
        assertTrue(browserScripts.matches(xhr));
    }

    @Test
    void theBrowserOnlyMatcherIgnoresTheClientSignals() {
        ProgrammaticRequestMatcher browserScripts = ProgrammaticRequestMatcher.ofBrowserScripts();
        MockHttpServletRequest bearer = request();
        bearer.addHeader("Authorization", "Bearer eyJ...");
        MockHttpServletRequest json = request();
        json.addHeader("Accept", "application/json, text/plain, */*");

        // a client that authenticates after a challenge must keep getting one
        assertFalse(browserScripts.matches(bearer));
        assertFalse(browserScripts.matches(json));
    }

    @Test
    void theBearerCheckIsCaseInsensitiveAndNeedsTheScheme() {
        assertTrue(ProgrammaticRequestMatcher.isBearerAuthorization("Bearer abc"));
        assertTrue(ProgrammaticRequestMatcher.isBearerAuthorization("BEARER abc"));
        assertFalse(ProgrammaticRequestMatcher.isBearerAuthorization("Bearerabc"));
        assertFalse(ProgrammaticRequestMatcher.isBearerAuthorization("Basic abc"));
        assertFalse(ProgrammaticRequestMatcher.isBearerAuthorization(null));
    }

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/services/ide/workspaces");
    }
}
