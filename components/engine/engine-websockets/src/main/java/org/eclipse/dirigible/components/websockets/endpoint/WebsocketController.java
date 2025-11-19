/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.endpoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.api.websockets.WebsocketsFacade;
import org.eclipse.dirigible.components.websockets.message.InputMessage;
import org.eclipse.dirigible.components.websockets.message.OutputMessage;
import org.eclipse.dirigible.components.websockets.service.WebsocketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * The Class WebsocketController.
 */
@Controller
public class WebsocketController {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WebsocketController.class);

    /** The processor. */
    private final WebsocketProcessor processor;

    /** The client outbound channel. */
    @Autowired
    @Qualifier("clientOutboundChannel")
    private MessageChannel clientOutboundChannel;

    /**
     * Instantiates a new websockets service.
     *
     * @param processor the processor
     */
    @Autowired
    public WebsocketController(WebsocketProcessor processor) {
        this.processor = processor;
    }

    /**
     * Gets the processor.
     *
     * @return the processor
     */
    public WebsocketProcessor getProcessor() {
        return processor;
    }

    /**
     * On message.
     *
     * @param endpoint the endpoint
     * @param message the message
     * @return the output message
     * @throws Exception the exception
     */
    @MessageMapping("/stomp/{endpoint}")
    @SendToUser("/queue/reply/{endpoint}")
    public OutputMessage onMessage(@DestinationVariable String endpoint, final InputMessage message) throws Exception {
        final String time = new SimpleDateFormat("HH:mm").format(new Date());
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[websocket] Endpoint '%s' received message:%s ", endpoint, message));
        }
        Map<Object, Object> context = new HashMap<>();
        context.put("message", message.getText());
        context.put("from", message.getFrom());
        context.put("method", "onmessage");
        try {
            Object result = getProcessor().processEvent(endpoint, context);
            return new OutputMessage(message.getFrom(), result != null ? result.toString() : "", time);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            return new OutputMessage(message.getFrom(), e.getMessage(), time);
        }
    }

    /**
     * Handle exception.
     *
     * @param endpoint the endpoint
     * @param throwable the throwable
     * @return the string
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors/{endpoint}")
    public String handleException(@DestinationVariable String endpoint, Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(String.format("[ws:console] Endpoint '%s' error %s", endpoint, throwable.getMessage()));
        }
        if (logger.isErrorEnabled()) {
            logger.error("[websocket] " + throwable.getMessage(), throwable);
        }
        Map<Object, Object> context = new HashMap<>();
        context.put("error", throwable.getMessage());
        context.put("method", "onerror");
        try {
            getProcessor().processEvent(endpoint, context);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return throwable.getMessage();
    }

}
