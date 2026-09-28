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

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * Records on a STOMP session that it was opened cross-origin, and from which origin.
 *
 * <p>
 * A fact, not a decision: the interceptor refuses nothing - Spring's own origin check, which runs
 * after it, refuses an origin that is not configured - and the CONNECT gate
 * ({@link BearerTokenStompInterceptor}) reads the mark to decide whether the identity the handshake
 * carried may become the session's user. The mark travels in the handshake attributes, which become
 * the session attributes of every frame. It is written on the raw WebSocket handshake and on the
 * SockJS transport request that creates a session, so the {@code xhr_streaming} and
 * {@code xhr_polling} fallbacks are covered as well as the WebSocket transport.
 *
 * <p>
 * Same-origin is what Spring means by it: a request without an {@code Origin} header - a
 * non-browser client - or one whose origin matches the scheme, host and port the request arrived
 * on. Behind a TLS-terminating proxy those differ from what the browser sent, so the platform's own
 * pages read as cross-origin here exactly as they do for the CORS filter - unless the platform is
 * told to trust the forwarded headers ({@code server.forward-headers-strategy}).
 */
class CrossOriginHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * The session attribute holding the origin a session was opened from, present when cross-origin.
     */
    static final String CROSS_ORIGIN_ATTRIBUTE = CrossOriginHandshakeInterceptor.class.getName() + ".ORIGIN";

    /**
     * Before handshake.
     *
     * @param request the handshake, or session-creating SockJS transport, request
     * @param response the response
     * @param wsHandler the handler
     * @param attributes the attributes the session is created with
     * @return always true
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!WebUtils.isSameOrigin(request)) {
            attributes.put(CROSS_ORIGIN_ATTRIBUTE, request.getHeaders()
                                                          .getOrigin());
        }
        return true;
    }

    /**
     * After handshake: nothing to do.
     *
     * @param request the request
     * @param response the response
     * @param wsHandler the handler
     * @param exception the failure, if any
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // the mark is written before the handshake; nothing to clean up
    }
}
