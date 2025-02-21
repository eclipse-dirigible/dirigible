/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.websockets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import jakarta.websocket.DeploymentException;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompSession;


/**
 * The Class WebsocketsFacade.
 */
public class WebsocketsFacade {

    /** The Constant DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_OPEN. */
    public static final String DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_OPEN = "net/wrappers/onOpen.js";

    /** The Constant DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_MESSAGE. */
    public static final String DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_MESSAGE = "net/wrappers/onMessage.js";

    /** The Constant DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_ERROR. */
    public static final String DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_ERROR = "net/wrappers/onError.js";

    /** The Constant DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_CLOSE. */
    public static final String DIRIGIBLE_WEBSOCKET_WRAPPER_MODULE_ON_CLOSE = "net/wrappers/onClose.js";

    /** The logger. */
    private static Logger logger = LoggerFactory.getLogger(WebsocketsFacade.class);

    /** The Constant CLIENTS. */
    public static final List<WebsocketClient> CLIENTS = Collections.synchronizedList(new ArrayList<WebsocketClient>());

    /**
     * Create a new Websocket by a given URI and Handler.
     *
     * @param uri the URI
     * @param handler the handler
     * @return the Websocket Session object
     * @throws DeploymentException in case of an error
     * @throws IOException in case of an error
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public static final StompSession createWebsocket(String uri, String handler)
            throws DeploymentException, IOException, InterruptedException, ExecutionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Connecting to " + uri);
        }
        WebsocketClient client = new WebsocketClient(uri, JavascriptService.get(), handler);
        StompSession session = client.connect();
        return session;
    }

    /**
     * Get all created clients.
     *
     * @return the list of clients
     */
    public static final List<WebsocketClient> getClients() {
        return CLIENTS;
    }

    /**
     * Get all created clients.
     *
     * @return the list in JSON
     */
    public static final String getClientsAsJson() {
        return GsonHelper.toJson(CLIENTS);
    }

    /**
     * Get a particular client by its session id.
     *
     * @param id the session id
     * @return the client
     */
    public static final WebsocketClient getClient(String id) {
        Optional<WebsocketClient> result = CLIENTS.stream()
                                                  .parallel()
                                                  .filter(client -> client.getSession()
                                                                          .getSessionId()
                                                                          .equals(id))
                                                  .findFirst();

        return result.isPresent() ? result.get() : null;
    }

    /**
     * Get a particular client by its handler.
     *
     * @param handler the handler
     * @return the client
     */
    public static final WebsocketClient getClientByHandler(String handler) {
        Optional<WebsocketClient> result = CLIENTS.stream()
                                                  .parallel()
                                                  .filter(client -> client.getHandler()
                                                                          .equals(handler))
                                                  .findFirst();
        return result.isPresent() ? result.get() : null;
    }


}
