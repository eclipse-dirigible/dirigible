/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.debug.java.handler;

import org.eclipse.dirigible.components.ide.debug.java.JavaDebugBridge;
import org.eclipse.dirigible.components.ide.debug.java.JavaDebugManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Spring WebSocket handler that bridges browser DAP clients to a per-workspace debug session
 * managed by {@link JavaDebugManager}.
 *
 * <p>
 * Expected WebSocket URL: {@code /websockets/ide/java-debug?workspace=<ws>}
 */
@Component
public class JavaDebugWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(JavaDebugWebSocketHandler.class);

    @Autowired
    private JavaDebugManager manager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = resolveUsername(session);
        String workspace = queryParam(session, "workspace");

        if (workspace == null) {
            logger.warn("[java-debug] Missing 'workspace' query param — closing session {}", session.getId());
            closeQuietly(session);
            return;
        }

        try {
            JavaDebugBridge bridge = manager.getOrStart(username, workspace);
            bridge.addSession(session);
        } catch (Exception e) {
            logger.error("[java-debug] Failed to start debug bridge for {}/{}", sanitize(username), sanitize(workspace), e);
            closeQuietly(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JavaDebugBridge bridge = manager.findForSession(session.getId());
        if (bridge != null) {
            bridge.sendToDap(message.getPayload());
        } else {
            logger.warn("[java-debug] Received message from unknown session {}", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable error) {
        logger.warn("[java-debug] Transport error on session {}: {}", session.getId(), error.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.debug("[java-debug] Session {} closed: {}", session.getId(), status);
        manager.removeSession(session.getId());
    }

    // -------------------------------------------------------------------------

    private static String resolveUsername(WebSocketSession session) {
        if (session.getPrincipal() != null) {
            return session.getPrincipal()
                          .getName();
        }
        return "anonymous";
    }

    private static String sanitize(String value) {
        return value == null ? null : value.replaceAll("[\r\n\t]", "_");
    }

    private static String queryParam(WebSocketSession session, String name) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.BAD_DATA);
        } catch (Exception ignored) {
        }
    }
}
