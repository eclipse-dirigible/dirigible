/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.client;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal Websocket Proxy Client.
 */
public class TerminalWebsocketClientEndpoint extends Endpoint {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketClientEndpoint.class);

    private static final String SUBPROTOCOL = "tty";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** The session. */
    private Session session = null;

    /** The message handler. */
    private MessageHandler messageHandler;

    /**
     * Instantiates a new x terminal websocket client endpoint.
     *
     * @param endpointURI the endpoint URI
     * @param authorization the Authorization header of the handshake
     */
    public TerminalWebsocketClientEndpoint(URI endpointURI, String authorization) {
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                                                                  .preferredSubprotocols(List.of(SUBPROTOCOL))
                                                                  .configurator(new ClientEndpointConfig.Configurator() {
                                                                      @Override
                                                                      public void beforeRequest(Map<String, List<String>> headers) {
                                                                          headers.put(AUTHORIZATION_HEADER, List.of(authorization));
                                                                      }
                                                                  })
                                                                  .build();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, config, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param session the session which is opened.
     * @param config the configuration the session was opened with
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        if (logger.isInfoEnabled()) {
            logger.info("[ws:terminal-client] connected: " + session.getId());
        }
        this.session = session;
        session.addMessageHandler(ByteBuffer.class, this::onMessage);
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param session the session which is getting closed.
     * @param reason the reason for connection close
     */
    @Override
    public void onClose(Session session, CloseReason reason) {
        if (logger.isInfoEnabled()) {
            logger.info("[ws:terminal-client] disconnected: " + session.getId());
        }
        this.session = null;
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message the message
     */
    private void onMessage(ByteBuffer message) {
        if (this.messageHandler != null) {
            try {
                this.messageHandler.handleMessage(message);
            } catch (IOException e) {
                logger.error("[ws:terminal-client] " + e.getMessage(), e);
            }
        }
    }

    /**
     * Register message handler.
     *
     * @param messageHandler the message handler
     */
    public void addMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Send a message.
     *
     * @param message the message
     */
    public void sendMessage(ByteBuffer message) {
        synchronized (this.session) {
            try {
                this.session.getBasicRemote()
                            .sendBinary(message);
            } catch (IOException e) {
                logger.error("[ws:terminal-client] " + e.getMessage(), e);
            }
        }
    }

    /**
     * Message handler.
     *
     */
    public static interface MessageHandler {

        /**
         * Handle message.
         *
         * @param message the message
         * @throws IOException Signals that an I/O exception has occurred.
         */
        public void handleMessage(ByteBuffer message) throws IOException;
    }
}
