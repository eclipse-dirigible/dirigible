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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * A state-changing request another site's page sends with the user's ambient credentials is
 * refused; everything a same-origin page, a server-side client or a bearer client sends passes.
 */
class CrossSiteRequestFilterTest {

    private static final String HOST = "app.example.com";

    @AfterEach
    void clearConfiguration() {
        Configuration.remove(DirigibleConfig.CORS_ALLOWED_ORIGINS.getKey());
    }

    @Test
    void aCrossSiteFormPostWithTheSessionCookieIsRefused() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");
        request.addHeader("Origin", "https://evil.example.org");

        MockHttpServletResponse response = run(request);

        assertEquals(403, response.getStatus());
    }

    @Test
    void aSameSiteSubdomainIsRefusedToo() throws Exception {
        // a sibling subdomain may be another tenant's
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "same-site");
        request.addHeader("Origin", "https://other.example.com");

        assertEquals(403, run(request).getStatus());
    }

    @Test
    void aSameOriginPostPasses() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "same-origin");
        request.addHeader("Origin", "https://" + HOST);

        assertPassed(request);
    }

    @Test
    void theUsersOwnNavigationPasses() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "none");

        assertPassed(request);
    }

    @Test
    void aSafeMethodPasses() throws Exception {
        MockHttpServletRequest request = withSession(new MockHttpServletRequest("GET", "/services/ts/app/gen/api/Invoice"));
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");

        assertPassed(request);
    }

    @Test
    void aCrossSitePostWithoutCredentialsPasses() throws Exception {
        MockHttpServletRequest request = post();
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");

        assertPassed(request);
    }

    @Test
    void browserRememberedBasicAuthenticationIsAmbient() throws Exception {
        MockHttpServletRequest request = post();
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");

        assertEquals(403, run(request).getStatus());
    }

    @Test
    void aBearerTokenIsNeverAmbient() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9");
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");

        assertPassed(request);
    }

    @Test
    void aServerSideClientPasses() throws Exception {
        // neither Sec-Fetch-Site nor Origin: not a browser page
        MockHttpServletRequest request = post();
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        assertPassed(request);
    }

    @Test
    void withoutFetchMetadataAForeignOriginIsRefused() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader("Origin", "https://evil.example.org");

        assertEquals(403, run(request).getStatus());
    }

    @Test
    void withoutFetchMetadataTheNullOriginIsRefused() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader("Origin", "null");

        assertEquals(403, run(request).getStatus());
    }

    @Test
    void withoutFetchMetadataTheOwnOriginPassesWhateverThePort() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.addHeader("Origin", "https://" + HOST + ":8443");

        assertPassed(request);
    }

    @Test
    void theForwardedHostIsTheOneTheBrowserAddressed() throws Exception {
        MockHttpServletRequest request = withSession(post());
        request.removeHeader("Host");
        request.addHeader("Host", "10.0.0.12:8080");
        request.addHeader(CrossSiteRequestFilter.X_FORWARDED_HOST, HOST);
        request.addHeader("Origin", "https://" + HOST);

        assertPassed(request);
    }

    @Test
    void aConfiguredOriginIsTrusted() throws Exception {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("https://front.example.org");
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");
        request.addHeader("Origin", "https://front.example.org");

        assertPassed(request);
    }

    @Test
    void theWildcardOriginTrustsNobody() throws Exception {
        DirigibleConfig.CORS_ALLOWED_ORIGINS.setStringValue("*");
        MockHttpServletRequest request = withSession(post());
        request.addHeader(CrossSiteRequestFilter.SEC_FETCH_SITE, "cross-site");
        request.addHeader("Origin", "https://evil.example.org");

        assertEquals(403, run(request).getStatus());
    }

    private static MockHttpServletRequest post() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/services/ts/app/gen/api/Invoice");
        request.setServerName(HOST);
        request.addHeader("Host", HOST);
        return request;
    }

    private static MockHttpServletRequest withSession(MockHttpServletRequest request) {
        request.setRequestedSessionId("4A2C1F");
        return request;
    }

    private static void assertPassed(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CrossSiteRequestFilter().doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "the request should have reached the chain");
    }

    private static MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CrossSiteRequestFilter().doFilter(request, response, chain);
        if (response.getStatus() == 403) {
            assertNull(chain.getRequest(), "a refused request must not reach the chain");
        }
        return response;
    }
}
