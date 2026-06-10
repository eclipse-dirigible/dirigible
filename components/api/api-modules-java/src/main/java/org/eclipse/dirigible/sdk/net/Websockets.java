/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.net;

import jakarta.websocket.DeploymentException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.dirigible.components.api.websockets.WebsocketClient;
import org.eclipse.dirigible.components.api.websockets.WebsocketsFacade;
import org.springframework.messaging.simp.stomp.StompSession;

/**
 * Outbound STOMP WebSocket client — connect to a remote endpoint, register a registry-resident
 * handler for lifecycle and message events, then optionally publish or query the platform's view of
 * active connections. Useful for bridging Dirigible into another service that exposes a WebSocket
 * interface (an external trading desk, a chat server, an MQTT bridge).
 * <p>
 * For inbound WebSockets (handlers exposed by Dirigible to remote clients) use the
 * {@link org.eclipse.dirigible.sdk.net.Websocket @Websocket} class annotation instead — that gives
 * you {@code onOpen}/{@code onMessage}/{@code onClose} entry points on a Java class.
 */
public final class Websockets {

    private Websockets() {}

    public static StompSession createWebsocket(String uri, String handler)
            throws DeploymentException, IOException, InterruptedException, ExecutionException {
        return WebsocketsFacade.createWebsocket(uri, handler);
    }

    public static List<WebsocketClient> getClients() {
        return WebsocketsFacade.getClients();
    }

    public static String getClientsAsJson() {
        return WebsocketsFacade.getClientsAsJson();
    }

    public static WebsocketClient getClient(String id) {
        return WebsocketsFacade.getClient(id);
    }

    public static WebsocketClient getClientByHandler(String handler) {
        return WebsocketsFacade.getClientByHandler(handler);
    }
}
