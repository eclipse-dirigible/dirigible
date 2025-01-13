/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.endpoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.data.transfer.callback.DataTransferCallbackHandler;
import org.eclipse.dirigible.components.data.transfer.callback.WriterDataTransferCallbackHandler;
import org.eclipse.dirigible.components.data.transfer.domain.DataTransfer;
import org.eclipse.dirigible.components.data.transfer.service.DataTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.JsonSyntaxException;

/**
 * The Data Transfer Websocket Service.
 */
public class DataTransferWebsocketHandler extends TextWebSocketHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataTransferWebsocketHandler.class);

    /** The data transfer service. */
    private final DataTransferService dataTransferService;

    /** The Constant HANDLERS. */
    private static final Map<String, DataTransferCallbackHandler> HANDLERS = new HashMap<String, DataTransferCallbackHandler>();

    /**
     * Instantiates a new data transfer websocket endpoint.
     *
     * @param dataTransferService the data transfer service
     */
    public DataTransferWebsocketHandler(DataTransferService dataTransferService) {
        this.dataTransferService = dataTransferService;
    }

    /**
     * Gets the data transfer service.
     *
     * @return the data transfer service
     */
    public DataTransferService getDataTransferService() {
        return dataTransferService;
    }

    /**
     * After connection established.
     *
     * @param session the session
     * @throws Exception the exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[ws:transfer] Session %s openned.", session.getId()));
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
            logger.debug(String.format("[ws:ws:transfer] Session %s received message: %s.", session.getId(), message));
        }
        DataTransferCallbackHandler maybeHandler = HANDLERS.get(session.getId());
        if (message != null && message.getPayload()
                                      .startsWith("{")) {
            // start

            if (maybeHandler != null) {
                maybeHandler.stopTransfer();
                HANDLERS.remove(session.getId());
            }
            try {
                DataTransfer definition = GsonHelper.fromJson(message.getPayload(), DataTransfer.class);

                PipedOutputStream pos = new PipedOutputStream();
                PipedInputStream pis = new PipedInputStream(pos, 1024);
                BufferedReader reader = new BufferedReader(new InputStreamReader(pis));
                Writer writer = new BufferedWriter(new OutputStreamWriter(pos));
                final DataTransferCallbackHandler handler =
                        new WriterDataTransferCallbackHandler(writer, session.getId() + new Date().getTime());
                HANDLERS.put(session.getId(), handler);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            getDataTransferService().transfer(definition, handler);
                            HANDLERS.remove(session.getId());
                            // reader.close();
                        } catch (Exception e) {
                            if (logger.isErrorEnabled()) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    public void run() {
                        reader.lines()
                              .forEach(line -> {
                                  try {
                                      session.sendMessage(new TextMessage(line));
                                  } catch (IOException e) {
                                      if (logger.isErrorEnabled()) {
                                          logger.error(e.getMessage(), e);
                                      }
                                  }
                              });
                    }
                }).start();

            } catch (JsonSyntaxException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            } catch (IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        } else {
            if ("stop".equalsIgnoreCase(message.getPayload())) {
                if (maybeHandler != null) {
                    maybeHandler.stopTransfer();
                    HANDLERS.remove(session.getId());
                }
            }
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
            logger.error(String.format("[ws:ws:transfer] Session %s error %s", session.getId(), exception.getMessage()));
        }
        if (logger.isErrorEnabled()) {
            logger.error("[ws:ws:transfer] " + exception.getMessage(), exception);
        }
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
            logger.debug(String.format("[ws:ws:transfer] Session %s closed because of %s", session.getId(), status.getReason()));
        }
    }

}
