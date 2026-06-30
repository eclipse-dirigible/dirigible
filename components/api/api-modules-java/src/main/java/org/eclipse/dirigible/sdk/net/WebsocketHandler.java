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

/**
 * Self-describing contract for a WebSocket handler — the strong-interface style. A
 * {@link org.eclipse.dirigible.sdk.component.Component @Component} bean that implements this
 * interface IS a WebSocket handler: it supplies its own endpoint via {@link #endpoint()} and
 * overrides only the lifecycle callbacks it needs (the rest inherit empty defaults), with no
 * {@code @Websocket} annotation. This mirrors extending {@code TextWebSocketHandler} in Spring —
 * the implementation carries everything.
 *
 * <p>
 * The alternative, annotation style is a {@code @Websocket(endpoint = …)} class with
 * {@code @OnOpen}/{@code @OnMessage}/{@code @OnError}/{@code @OnClose} methods (the endpoint has no
 * method-level home, so the class annotation carries it — like Jakarta {@code @ServerEndpoint}). A
 * single class uses one style or the other, never both.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class ChatHandler implements WebsocketHandler {
 *     public String endpoint() { return "java-chat"; }
 *     {@literal @}Override public void onMessage(String message, String from) { ... }
 *     // onOpen, onError, onClose inherit the no-op default
 * }
 * </pre>
 */
public interface WebsocketHandler {

    /**
     * The endpoint suffix this handler binds to, e.g. {@code "java-chat"} maps to
     * {@code /websockets/stomp/java-chat}.
     *
     * @return the endpoint suffix
     */
    String endpoint();

    /** Fires once when a client opens the WebSocket. */
    default void onOpen() {
        // intentional no-op default
    }

    /**
     * Fires for every inbound text frame.
     *
     * @param message the text payload
     * @param from a stable identifier for the originating session
     */
    default void onMessage(String message, String from) {
        // intentional no-op default
    }

    /** Fires when the underlying transport reports an error. */
    default void onError(String error) {
        // intentional no-op default
    }

    /** Fires once when the client closes the WebSocket. */
    default void onClose() {
        // intentional no-op default
    }
}
