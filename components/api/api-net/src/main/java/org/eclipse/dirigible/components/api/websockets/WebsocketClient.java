/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.websockets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * The Class WebsocketClient.
 */
@ClientEndpoint
public class WebsocketClient {

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(WebsocketClient.class);

    /** The uri. */
    private String uri;

    /** The handler. */
    private String handler;

    /** The session. */
    private StompSession session;

    /** The javascript service. */
    private final JavascriptService javascriptService;

    /**
     * Instantiates a new websocket client.
     *
     * @param uri the uri
     * @param javascriptService the javascript service
     * @param handler the handler
     */
    public WebsocketClient(String uri, JavascriptService javascriptService, String handler) {
        this.uri = uri;
        this.javascriptService = javascriptService;
        this.handler = handler;
    }

    /**
     * Connect.
     *
     * @return the stomp session
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public StompSession connect() throws InterruptedException, ExecutionException {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSessionHandler sessionHandler = new ClientStompSessionHandler();
        session = stompClient.connect(uri, sessionHandler)
                             .get();
        return session;
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Gets the javascript service.
     *
     * @return the javascript service
     */
    public JavascriptService getJavascriptService() {
        return javascriptService;
    }

    /**
     * Gets the handler.
     *
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Gets the session.
     *
     * @return the session
     */
    public StompSession getSession() {
        return session;
    }

    /**
     * On open.
     *
     * @param session the session
     * @throws Exception the scripting exception
     */
    @OnOpen
    public void onOpen(StompSession session) throws Exception {
        this.session = session;
        WebsocketsFacade.CLIENTS.add(this);
        Map<Object, Object> context = new HashMap<>();
        context.put("method", "onopen");
        context.put("handler", this.handler);
        RepositoryPath path = new RepositoryPath(handler);
        getJavascriptService().handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
    }

    /**
     * Process message.
     *
     * @param message the message
     * @throws Exception the scripting exception
     */
    @OnMessage
    public void processMessage(String message) throws Exception {
        Map<Object, Object> context = new HashMap<>();
        context.put("message", message);
        context.put("method", "onmessage");
        context.put("handler", this.handler);
        RepositoryPath path = new RepositoryPath(WebsocketsFacade.DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_MESSAGE);
        getJavascriptService().handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
    }

    /**
     * Process error.
     *
     * @param t the t
     * @throws Exception the scripting exception
     */
    @OnError
    public void processError(Throwable t) throws Exception {
        if (logger.isErrorEnabled()) {
            logger.error(t.getMessage(), t);
        }
        Map<Object, Object> context = new HashMap<>();
        context.put("error", t.getMessage());
        context.put("method", "onerror");
        context.put("handler", this.handler);
        RepositoryPath path = new RepositoryPath(WebsocketsFacade.DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_ERROR);
        getJavascriptService().handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
    }

    /**
     * On close.
     *
     * @param session the session
     * @throws Exception the scripting exception
     */
    @OnClose
    public void onClose(Session session) throws Exception {
        WebsocketsFacade.CLIENTS.remove(this);
        Map<Object, Object> context = new HashMap<>();
        context.put("method", "onclose");
        context.put("handler", this.handler);
        RepositoryPath path = new RepositoryPath(WebsocketsFacade.DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_CLOSE);
        getJavascriptService().handleRequest(path.getSegments()[0], path.constructPathFrom(1), null, context, false);
    }

}
