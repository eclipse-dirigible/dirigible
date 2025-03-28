/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import org.eclipse.dirigible.components.terminal.client.TerminalWebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.dirigible.components.terminal.endpoint.TerminalWebsocketConfig.TERMINAL_PREFIX;

/**
 * The Console Websocket Handler.
 */
class TerminalWebsocketHandler extends BinaryWebSocketHandler implements SubProtocolCapable {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketHandler.class);

    /** The open sessions. */
    private static final Map<String, WebSocketSession> OPEN_SESSIONS = new ConcurrentHashMap<>();

    /** The session to client. */
    private static final Map<String, TerminalWebsocketClientEndpoint> SESSION_TO_CLIENT = new ConcurrentHashMap<>();

    /**
     * After connection established.
     *
     * @param session the session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.debug("[ws:terminal] onOpen: {}", session.getId());
        try {
            TerminalWebsocketClientEndpoint clientEndPoint = startClientWebsocket(session);
            SESSION_TO_CLIENT.put(session.getId(), clientEndPoint);
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
            try {
                session.close();
            } catch (Exception e1) {
                logger.error(TERMINAL_PREFIX, e.getMessage(), e);
            }
        }
        OPEN_SESSIONS.put(session.getId(), session);
    }

    /**
     * Start the WebSocket proxy.
     *
     * @param session the source session
     * @return the x terminal websocket client endpoint
     * @throws URISyntaxException the URI syntax exception
     */
    private TerminalWebsocketClientEndpoint startClientWebsocket(WebSocketSession session) throws URISyntaxException {

        final TerminalWebsocketClientEndpoint clientEndPoint = new TerminalWebsocketClientEndpoint(new URI("ws://localhost:9000/ws"));

        // add listener
        clientEndPoint.addMessageHandler(message -> session.sendMessage(new BinaryMessage(message)));

        return clientEndPoint;
    }

    /**
     * Handle text message.
     *
     * @param session the session
     * @param message the message
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        logger.trace("[ws:terminal] onMessage: {}", new String(message.getPayload()
                                                                      .array()));

        TerminalWebsocketClientEndpoint clientEndPoint = SESSION_TO_CLIENT.get(session.getId());

        if (clientEndPoint != null) {
            synchronized (clientEndPoint) {
                // send message to websocket
                clientEndPoint.sendMessage(message.getPayload());
            }
        }
    }

    /**
     * Handle transport error.
     *
     * @param session the session
     * @param throwable the throwable
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) {
        if (logger.isInfoEnabled()) {
            logger.info("[ws:terminal] Session [{}] error [{}]", session.getId(), throwable.getMessage());
        }
    }

    /**
     * After connection closed.
     *
     * @param session the session
     * @param status the status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.trace("[ws:terminal] Session [{}] closed because of [{}]", session.getId(), status.getReason());
        OPEN_SESSIONS.remove(session.getId());
        TerminalWebsocketClientEndpoint clientEndPoint = SESSION_TO_CLIENT.remove(session.getId());
        try {
            if (clientEndPoint != null && clientEndPoint.getSession() != null) {
                clientEndPoint.getSession()
                              .close();
            }
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX, e.getMessage(), e);
        }
    }

    /**
     * Gets the sub protocols.
     *
     * @return the sub protocols
     */
    @Override
    public List<String> getSubProtocols() {
        return List.of("tty");
    }

}
