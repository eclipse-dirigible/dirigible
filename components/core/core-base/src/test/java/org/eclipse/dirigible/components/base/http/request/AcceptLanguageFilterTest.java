/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * A request whose {@code Accept-Language} the container refuses is a 400, never an unhandled 500.
 */
class AcceptLanguageFilterTest {

    private final AcceptLanguageFilter filter = new AcceptLanguageFilter();

    @Test
    void aRefusedHeaderIsA400AndTheChainIsNotEntered() throws Exception {
        MockHttpServletRequest request = new RefusingLocalesRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString()
                           .contains(AcceptLanguageFilter.MESSAGE));
        assertNull(chain.getRequest(), "the chain - RequestContextFilter among it - is not entered");
    }

    @Test
    void anAcceptableHeaderPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Accept-Language", "en-US,en;q=0.9,de;q=0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    /** Fails as Tomcat's {@code Request.parseLocales} does above the connector's maxLocaleCount. */
    private static final class RefusingLocalesRequest extends MockHttpServletRequest {

        RefusingLocalesRequest() {
            super("GET", "/");
        }

        @Override
        public Locale getLocale() {
            throw refusal();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            throw refusal();
        }

        private static IllegalArgumentException refusal() {
            return new IllegalArgumentException("More than the maximum allowed number of locales, [10], were detected.");
        }
    }
}
