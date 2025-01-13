/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.console.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * The Console Websocket Handler.
 */
public class ConsoleWebsocketHandler extends TextWebSocketHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ConsoleWebsocketHandler.class);

    /** The open sessions. */
    private static Map<String, WebSocketSession> OPEN_SESSIONS = new ConcurrentHashMap<String, WebSocketSession>();


    /**
     * After connection established.
     *
     * @param session the session
     * @throws Exception the exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        OPEN_SESSIONS.put(session.getId(), session);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[ws:console] Session %s openned.", session.getId()));
        }
    }

    /**
     * Handle text message.
     *
     * @param session the session
     * @param message the message
     * @throws Exception the exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[ws:console] Session %s received message: %s.", session.getId(), message));
        }
    }

    /**
     * Handle transport error.
     *
     * @param session the session
     * @param exception the exception
     * @throws Exception the exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (logger.isErrorEnabled()) {
            logger.error(String.format("[ws:console] Session %s error %s", session.getId(), exception.getMessage()));
        }
        // if (logger.isErrorEnabled()) {
        // logger.error("[ws:console] " + exception.getMessage(), exception);
        // }
    }

    /**
     * After connection closed.
     *
     * @param session the session
     * @param status the status
     * @throws Exception the exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[ws:console] Session %s closed because of %s", session.getId(), status.getReason()));
        }
        OPEN_SESSIONS.remove(session.getId());
    }

    /**
     * Distribute message to all the listeners.
     *
     * @param record the record
     */
    public static void distribute(ConsoleLogRecord record) {
        for (WebSocketSession session : OPEN_SESSIONS.values()) {
            synchronized (session) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(GsonHelper.toJson(record)));
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

}
