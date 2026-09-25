/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

/**
 * A handshake from another origin is marked with that origin; a same-origin one - matching scheme,
 * host and port, or carrying no {@code Origin} at all - is not. Nothing is refused here.
 */
class CrossOriginHandshakeInterceptorTest {

    private final CrossOriginHandshakeInterceptor interceptor = new CrossOriginHandshakeInterceptor();
    private final Map<String, Object> attributes = new HashMap<>();

    @Test
    void aRequestWithoutAnOriginIsSameOrigin() {
        // a non-browser client - the platform's own websockets API, a Java client - sends none
        assertTrue(handshake(null));
        assertFalse(attributes.containsKey(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE));
    }

    @Test
    void anOriginMatchingTheRequestIsSameOrigin() {
        assertTrue(handshake("http://localhost"));
        assertFalse(attributes.containsKey(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE));
    }

    @Test
    void anotherOriginIsMarkedWithItself() {
        assertTrue(handshake("https://app.example.com"), "the mark is a fact, the origin check decides");
        assertEquals("https://app.example.com", attributes.get(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE));
    }

    @Test
    void anotherSchemeOnTheSameHostIsCrossOrigin() {
        // what the platform's own pages look like behind a TLS-terminating proxy that forwards no
        // headers: the browser says https, the request arrived over http
        assertTrue(handshake("https://localhost"));
        assertEquals("https://localhost", attributes.get(CrossOriginHandshakeInterceptor.CROSS_ORIGIN_ATTRIBUTE));
    }

    private boolean handshake(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stomp");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return interceptor.beforeHandshake(new ServletServerHttpRequest(request), mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class), attributes);
    }
}
