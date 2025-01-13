/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.components.terminal.client.TerminalWebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * The Console Websocket Handler.
 */
public class TerminalWebsocketHandler extends BinaryWebSocketHandler implements SubProtocolCapable {

    /** The Constant TERMINAL_PREFIX. */
    private static final String TERMINAL_PREFIX = "[ws:terminal] ";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TerminalWebsocketHandler.class);

    /** The open sessions. */
    private static Map<String, WebSocketSession> OPEN_SESSIONS = new ConcurrentHashMap<String, WebSocketSession>();

    /** The session to client. */
    private static Map<String, TerminalWebsocketClientEndpoint> SESSION_TO_CLIENT =
            new ConcurrentHashMap<String, TerminalWebsocketClientEndpoint>();


    /**
     * After connection established.
     *
     * @param session the session
     * @throws Exception the exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // if (Configuration.isAnonymousModeEnabled() || !Configuration.isTerminalEnabled()) {
        // try {
        // session.getBasicRemote().sendText(FEATURE_TERMINAL_IS_DISABLED_IN_THIS_MODE, true);
        // if (logger.isWarnEnabled()) {logger.warn(FEATURE_TERMINAL_IS_DISABLED_IN_THIS_MODE);}
        // } catch (IOException e) {
        // logger.error(TERMINAL_PREFIX + e.getMessage(), e);
        // }
        // try {
        // session.close();
        // } catch (IOException e) {
        // logger.error(TERMINAL_PREFIX + e.getMessage(), e);
        // }
        // return;
        // }

        if (logger.isDebugEnabled()) {
            logger.debug("[ws:terminal] onOpen: " + session.getId());
        }
        try {
            TerminalWebsocketClientEndpoint clientEndPoint = startClientWebsocket(session);
            SESSION_TO_CLIENT.put(session.getId(), clientEndPoint);
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX + e.getMessage(), e);
            try {
                session.close();
            } catch (Exception e1) {
                logger.error(TERMINAL_PREFIX + e.getMessage(), e);
            }
        }
        OPEN_SESSIONS.put(session.getId(), session);
    }

    /**
     * Handle text message.
     *
     * @param session the session
     * @param message the message
     * @throws Exception the exception
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("[ws:terminal] onMessage: " + new String(message.getPayload()
                                                                         .array()));
        }

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
     * @throws Exception the exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("[ws:terminal] Session %s error %s", session.getId(), throwable.getMessage()));
        }
        // logger.error(TERMINAL_PREFIX + throwable.getMessage(), throwable);
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
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[ws:terminal] Session %s closed because of %s", session.getId(), status.getReason()));
        }
        OPEN_SESSIONS.remove(session.getId());
        TerminalWebsocketClientEndpoint clientEndPoint = SESSION_TO_CLIENT.remove(session.getId());
        try {
            if (clientEndPoint != null && clientEndPoint.getSession() != null) {
                clientEndPoint.getSession()
                              .close();
            }
        } catch (Exception e) {
            logger.error(TERMINAL_PREFIX + e.getMessage(), e);
        }
    }

    /**
     * Gets the sub protocols.
     *
     * @return the sub protocols
     */
    @Override
    public List<String> getSubProtocols() {
        return Arrays.asList("tty");
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
        clientEndPoint.addMessageHandler(new TerminalWebsocketClientEndpoint.MessageHandler() {
            public void handleMessage(ByteBuffer message) throws IOException {
                session.sendMessage(new BinaryMessage(message));
            }
        });

        return clientEndPoint;
    }

    /**
     * The Process Runnable.
     */
    static class ProcessRunnable implements Runnable {

        /** The command. */
        private String command;

        /** The process. */
        private Process process;

        /**
         * Instantiates a new process runnable.
         *
         * @param command the command
         */
        ProcessRunnable(String command) {
            this.command = command;
        }

        /**
         * Gets the process.
         *
         * @return the process
         */
        public Process getProcess() {
            return process;
        }

        /**
         * Run.
         */
        @Override
        public void run() {
            try {
                this.process = Runtime.getRuntime()
                                      .exec(this.command);

                Thread reader = new Thread(new Runnable() {
                    public void run() {
                        try {
                            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;

                                while ((line = input.readLine()) != null) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(TERMINAL_PREFIX + line);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            logger.error(TERMINAL_PREFIX + e.getMessage(), e);
                        }
                    }
                });
                reader.start();


                Thread error = new Thread(new Runnable() {
                    public void run() {
                        try {
                            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                String line;

                                while ((line = input.readLine()) != null) {
                                    logger.error(TERMINAL_PREFIX + line);
                                }
                            }
                        } catch (IOException e) {
                            logger.error(TERMINAL_PREFIX + e.getMessage(), e);
                        }
                    }
                });
                error.start();

                // logger.info("[ws:terminal] " + process.exitValue());
            } catch (IOException e) {
                logger.error(TERMINAL_PREFIX + e.getMessage(), e);
            }

        }

    }

}
